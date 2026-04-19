package br.com.dealership.clientapi.service;

import br.com.dealership.clientapi.client.ViaCepClient;
import br.com.dealership.clientapi.client.dto.ViaCepResponse;
import br.com.dealership.clientapi.dto.request.CreateClientRequest;
import br.com.dealership.clientapi.dto.request.UpdateClientRequest;
import br.com.dealership.clientapi.dto.request.UpdateCpfRequest;
import br.com.dealership.clientapi.dto.response.ClientResponse;
import br.com.dealership.clientapi.entity.Client;
import br.com.dealership.clientapi.exception.ClientNotFoundException;
import br.com.dealership.clientapi.exception.DuplicateCpfException;
import br.com.dealership.clientapi.exception.DuplicateKeycloakIdException;
import br.com.dealership.clientapi.exception.ProfileInactiveException;
import br.com.dealership.clientapi.persistence.CpfHashUtil;
import br.com.dealership.clientapi.repository.ClientRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ClientService {

    public static final String ANONYMIZED = "ANONYMIZED";
    public static final String PROFILE_NOT_FOUND_MESSAGE = "Profile not found";
    private final ClientRepository repository;
    private final CpfHashUtil cpfHashUtil;
    private final ViaCepClient viaCepClient;
    private final CacheManager cacheManager;

    public ClientService(ClientRepository repository, CpfHashUtil cpfHashUtil,
                         ViaCepClient viaCepClient, CacheManager cacheManager) {
        this.repository = repository;
        this.cpfHashUtil = cpfHashUtil;
        this.viaCepClient = viaCepClient;
        this.cacheManager = cacheManager;
    }

    // ─── US1: Register ────────────────────────────────────────────────────────

    public ClientResponse createClient(CreateClientRequest request) {
        if (repository.existsByKeycloakId(request.keycloakId())) {
            throw new DuplicateKeycloakIdException("A profile already exists for this Keycloak account");
        }
        final String cpfHash = cpfHashUtil.hash(request.cpf());
        if (repository.existsByCpfHash(cpfHash)) {
            throw new DuplicateCpfException("A profile already exists for this CPF", request.cpf());
        }

        final Optional<ViaCepResponse> viaCep = viaCepClient.lookupPostcode(request.postcode());
        final Client.Address address = viaCep
                .map(v -> Client.Address.from(v, request.postcode(), request.streetNumber()))
                .orElseGet(() -> Client.Address.from(null, request.postcode(), request.streetNumber()));

        final Client client = Client.builder()
                .keycloakId(request.keycloakId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .cpf(request.cpf())
                .cpfHash(cpfHash)
                .phoneNumber(request.phoneNumber())
                .address(address)
                .build();

        return ClientResponse.from(repository.save(client));
    }

    // ─── US2: View Own Profile ────────────────────────────────────────────────

    @Cacheable(cacheNames = "clients", key = "#keycloakId")
    @Transactional(readOnly = true)
    public ClientResponse getMyProfile(String keycloakId) {
        final Client client = repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ClientNotFoundException(PROFILE_NOT_FOUND_MESSAGE));
        return ClientResponse.from(client);
    }

    // ─── US3: Update Profile (personal + address) ─────────────────────────────

    public ClientResponse updateClient(UUID id, UpdateClientRequest request, String requesterKeycloakId) {
        final Client client = repository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(PROFILE_NOT_FOUND_MESSAGE));

        if (client.getDeletedAt() != null) {
            throw new ProfileInactiveException("Profile is inactive and cannot be updated");
        }

        final boolean isSystem = isSystemRole();

        if (!isSystem && !client.getKeycloakId().equals(requesterKeycloakId)) {
            throw new ClientNotFoundException(PROFILE_NOT_FOUND_MESSAGE);
        }

        final var builder = client.toBuilder();
        if (request.firstName() != null) builder.firstName(request.firstName());
        if (request.lastName() != null) builder.lastName(request.lastName());
        if (request.phoneNumber() != null) builder.phoneNumber(request.phoneNumber());

        if (request.postcode() != null) {
            final Optional<ViaCepResponse> viaCep = viaCepClient.lookupPostcode(request.postcode());
            final Client.Address updatedAddress = viaCep
                    .map(v -> Client.Address.from(v, request.postcode(), request.streetNumber()))
                    .orElseGet(() -> Client.Address.from(null, request.postcode(), request.streetNumber()));
            builder.address(updatedAddress);
        }

        final Client saved = repository.save(builder.build());
        evictClientCache(saved.getKeycloakId());
        return ClientResponse.from(saved);
    }

    // ─── US4: Admin CPF Correction────────────────────────────────────────────

    public ClientResponse correctCpf(UUID id, UpdateCpfRequest request) {
        final Client client = repository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(PROFILE_NOT_FOUND_MESSAGE));

        final String newCpfHash = cpfHashUtil.hash(request.cpf());
        if (repository.existsByCpfHash(newCpfHash)) {
            throw new DuplicateCpfException("A profile already exists for this CPF", request.cpf());
        }

        final Client saved = repository.save(client.toBuilder()
                .cpf(request.cpf())
                .cpfHash(newCpfHash)
                .build());
        evictClientCache(saved.getKeycloakId());
        return ClientResponse.from(saved);
    }

    // ─── US5: Delete(Anonymize) ──────────────────────────────────────────────

    public void deleteClient(UUID id, String requesterKeycloakId) {
        final Client client = repository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(PROFILE_NOT_FOUND_MESSAGE));

        if (client.getDeletedAt() != null) {
            throw new ProfileInactiveException("Profile is already inactive");
        }

        if (!client.getKeycloakId().equals(requesterKeycloakId)) {
            throw new ClientNotFoundException(PROFILE_NOT_FOUND_MESSAGE);
        }

        final String originalKeycloakId = client.getKeycloakId();

        repository.save(client.toBuilder()
                .firstName(ANONYMIZED)
                .lastName(ANONYMIZED)
                .phoneNumber(ANONYMIZED)
                .cpf(UUID.randomUUID().toString())
                .cpfHash(UUID.randomUUID().toString())
                .keycloakId(UUID.randomUUID().toString())
                .address(Client.Address.builder()
                        .postcode("")
                        .streetNumber("")
                        .streetName("")
                        .city("")
                        .state("")
                        .addressSearched(false)
                        .build())
                .deletedAt(LocalDateTime.now())
                .build());
        evictClientCache(originalKeycloakId);
    }

    // ─── Cache helpers ────────────────────────────────────────────────────────

    private void evictClientCache(String keycloakId) {
        final var cache = cacheManager.getCache("clients");
        if (cache != null) {
            cache.evict(keycloakId);
        }
    }

    private boolean isSystemRole() {
        final var context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final var authentication = context.getAuthentication();

            return authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SYSTEM".equals(a.getAuthority()));
        }

        return false;
    }
}
