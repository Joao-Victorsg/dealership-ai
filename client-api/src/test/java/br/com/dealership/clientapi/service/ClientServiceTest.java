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
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository repository;
    @Mock
    private CpfHashUtil cpfHashUtil;
    @Mock
    private ViaCepClient viaCepClient;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private ClientService clientService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── createClient ─────────────────────────────────────────────────────────

    @Test
    void createClientShouldReturnResponseWithAddressSearchedTrueWhenViaCepSucceeds() {
        final var request = Instancio.create(CreateClientRequest.class);
        final var viacepResponse = new ViaCepResponse("01310-100", "Av. Paulista", "São Paulo", "SP", null);
        final var savedClient = Instancio.create(Client.class);

        when(repository.existsByKeycloakId(request.keycloakId())).thenReturn(false);
        when(cpfHashUtil.hash(request.cpf())).thenReturn("hashvalue");
        when(repository.existsByCpfHash("hashvalue")).thenReturn(false);
        when(viaCepClient.lookupPostcode(request.postcode())).thenReturn(Optional.of(viacepResponse));
        when(repository.save(any(Client.class))).thenReturn(savedClient);

        final var result = assertDoesNotThrow(() -> clientService.createClient(request));

        assertEquals(ClientResponse.from(savedClient), result);
    }

    @Test
    void createClientShouldCreateProfileWithAddressSearchedFalseWhenViaCepFails() {
        final var request = Instancio.create(CreateClientRequest.class);
        final var savedClient = Instancio.create(Client.class);

        when(repository.existsByKeycloakId(request.keycloakId())).thenReturn(false);
        when(cpfHashUtil.hash(request.cpf())).thenReturn("hashvalue");
        when(repository.existsByCpfHash("hashvalue")).thenReturn(false);
        when(viaCepClient.lookupPostcode(request.postcode())).thenReturn(Optional.empty());
        when(repository.save(any(Client.class))).thenReturn(savedClient);

        final var result = assertDoesNotThrow(() -> clientService.createClient(request));

        assertNotNull(result);
    }

    @Test
    void createClientShouldThrowDuplicateKeycloakIdExceptionWhenKeycloakIdExists() {
        final var request = Instancio.create(CreateClientRequest.class);
        when(repository.existsByKeycloakId(request.keycloakId())).thenReturn(true);

        assertThrows(DuplicateKeycloakIdException.class, () -> clientService.createClient(request));
        verifyNoInteractions(cpfHashUtil, viaCepClient);
    }

    @Test
    void createClientShouldThrowDuplicateCpfExceptionWhenCpfHashExists() {
        final var request = Instancio.create(CreateClientRequest.class);
        when(repository.existsByKeycloakId(request.keycloakId())).thenReturn(false);
        when(cpfHashUtil.hash(request.cpf())).thenReturn("existingHash");
        when(repository.existsByCpfHash("existingHash")).thenReturn(true);

        assertThrows(DuplicateCpfException.class, () -> clientService.createClient(request));
        verifyNoInteractions(viaCepClient);
    }

    // ─── getMyProfile ─────────────────────────────────────────────────────────

    @Test
    void getMyProfileShouldReturnResponseWhenProfileFound() {
        final var keycloakId = UUID.randomUUID().toString();
        final var client = Instancio.create(Client.class);

        when(repository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(client));

        final var result = assertDoesNotThrow(() -> clientService.getMyProfile(keycloakId));

        assertEquals(ClientResponse.from(client), result);
        verify(repository).findByKeycloakId(keycloakId);
    }

    @Test
    void getMyProfileShouldThrowClientNotFoundExceptionWhenProfileAbsent() {
        final var keycloakId = UUID.randomUUID().toString();
        when(repository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.getMyProfile(keycloakId));
    }

    // ─── updateClient ─────────────────────────────────────────────────────────

    @Test
    void updateClientShouldUpdatePersonalFieldsAndEvictCache() {
        final var id = UUID.randomUUID();
        final var keycloakId = UUID.randomUUID().toString();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), keycloakId)
                .set(Select.field(Client::getDeletedAt), null)
                .create();
        final var request = new UpdateClientRequest("NewFirst", "NewLast", "+55 11 99999-9999", null, null);
        final var savedClient = Instancio.create(Client.class);

        givenClientAuthority("ROLE_CLIENT", keycloakId);
        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(repository.save(any(Client.class))).thenReturn(savedClient);
        when(cacheManager.getCache("clients")).thenReturn(cache);

        final var result = assertDoesNotThrow(() -> clientService.updateClient(id, request, keycloakId));

        assertEquals(ClientResponse.from(savedClient), result);
        verify(cache).evict(savedClient.getKeycloakId());
    }

    @Test
    void updateClientShouldSetAddressSearchedTrueWhenViaCepSucceeds() {
        final var id = UUID.randomUUID();
        final var keycloakId = UUID.randomUUID().toString();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), keycloakId)
                .set(Select.field(Client::getDeletedAt), null)
                .create();
        final var request = new UpdateClientRequest(null, null, null, "01310-100", "100");
        final var viacepResponse = new ViaCepResponse("01310-100", "Av. Paulista", "São Paulo", "SP", null);
        final var savedClient = Instancio.create(Client.class);

        givenClientAuthority("ROLE_CLIENT", keycloakId);
        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(viaCepClient.lookupPostcode("01310-100")).thenReturn(Optional.of(viacepResponse));
        when(repository.save(any(Client.class))).thenReturn(savedClient);
        when(cacheManager.getCache("clients")).thenReturn(cache);

        final var result = assertDoesNotThrow(() -> clientService.updateClient(id, request, keycloakId));

        assertNotNull(result);
    }

    @Test
    void updateClientShouldSetAddressSearchedFalseWhenViaCepFails() {
        final var id = UUID.randomUUID();
        final var keycloakId = UUID.randomUUID().toString();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), keycloakId)
                .set(Select.field(Client::getDeletedAt), null)
                .create();
        final var request = new UpdateClientRequest(null, null, null, "99999-999", "1");
        final var savedClient = Instancio.create(Client.class);

        givenClientAuthority("ROLE_CLIENT", keycloakId);
        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(viaCepClient.lookupPostcode("99999-999")).thenReturn(Optional.empty());
        when(repository.save(any(Client.class))).thenReturn(savedClient);
        when(cacheManager.getCache("clients")).thenReturn(cache);

        assertDoesNotThrow(() -> clientService.updateClient(id, request, keycloakId));
    }

    @Test
    void updateClientShouldThrowProfileInactiveExceptionWhenProfileDeleted() {
        final var id = UUID.randomUUID();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getDeletedAt), LocalDateTime.now())
                .create();

        when(repository.findById(id)).thenReturn(Optional.of(client));

        assertThrows(ProfileInactiveException.class,
                () -> clientService.updateClient(id, Instancio.create(UpdateClientRequest.class), "any"));
    }

    @Test
    void updateClientShouldThrowClientNotFoundExceptionWhenOwnershipMismatch() {
        final var id = UUID.randomUUID();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), "owner-id")
                .set(Select.field(Client::getDeletedAt), null)
                .create();

        givenClientAuthority("ROLE_CLIENT", "different-requester");
        when(repository.findById(id)).thenReturn(Optional.of(client));

        assertThrows(ClientNotFoundException.class,
                () -> clientService.updateClient(id, Instancio.create(UpdateClientRequest.class), "different-requester"));
    }

    @Test
    void updateClientShouldBypassOwnershipCheckForRoleSystem() {
        final var id = UUID.randomUUID();
        final var systemCallerId = UUID.randomUUID().toString();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), "some-other-client")
                .set(Select.field(Client::getDeletedAt), null)
                .create();
        final var request = new UpdateClientRequest(null, null, null, "01310-100", "1");
        final var savedClient = Instancio.create(Client.class);

        givenClientAuthority("ROLE_SYSTEM", systemCallerId);
        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(viaCepClient.lookupPostcode("01310-100")).thenReturn(Optional.empty());
        when(repository.save(any(Client.class))).thenReturn(savedClient);
        when(cacheManager.getCache("clients")).thenReturn(cache);

        assertDoesNotThrow(() -> clientService.updateClient(id, request, systemCallerId));
    }

    // ─── correctCpf ──────────────────────────────────────────────────────────

    @Test
    void correctCpfShouldUpdateCpfAndEvictCache() {
        final var id = UUID.randomUUID();
        final var client = Instancio.create(Client.class);
        final var request = new UpdateCpfRequest("12345678901");
        final var savedClient = Instancio.create(Client.class);

        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(cpfHashUtil.hash("12345678901")).thenReturn("newHash");
        when(repository.existsByCpfHash("newHash")).thenReturn(false);
        when(repository.save(any(Client.class))).thenReturn(savedClient);
        when(cacheManager.getCache("clients")).thenReturn(cache);

        final var result = assertDoesNotThrow(() -> clientService.correctCpf(id, request));

        assertEquals(ClientResponse.from(savedClient), result);
        verify(cache).evict(savedClient.getKeycloakId());
    }

    @Test
    void correctCpfShouldThrowDuplicateCpfExceptionWhenNewCpfAlreadyExists() {
        final var id = UUID.randomUUID();
        final var client = Instancio.create(Client.class);
        final var request = new UpdateCpfRequest("12345678901");

        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(cpfHashUtil.hash("12345678901")).thenReturn("existingHash");
        when(repository.existsByCpfHash("existingHash")).thenReturn(true);

        assertThrows(DuplicateCpfException.class, () -> clientService.correctCpf(id, request));
    }

    @Test
    void correctCpfShouldThrowClientNotFoundExceptionWhenClientAbsent() {
        final var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class,
                () -> clientService.correctCpf(id, new UpdateCpfRequest("12345678901")));
    }

    // ─── deleteClient ─────────────────────────────────────────────────────────

    @Test
    void deleteClientShouldAnonymizeAllPiiFieldsAndEvictCache() {
        final var id = UUID.randomUUID();
        final var keycloakId = UUID.randomUUID().toString();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), keycloakId)
                .set(Select.field(Client::getDeletedAt), null)
                .create();

        when(repository.findById(id)).thenReturn(Optional.of(client));
        when(repository.save(any(Client.class))).thenReturn(client);
        when(cacheManager.getCache("clients")).thenReturn(cache);

        assertDoesNotThrow(() -> clientService.deleteClient(id, keycloakId));

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(repository).save(captor.capture());
        Client saved = captor.getValue();

        assertEquals("ANONYMIZED", saved.getFirstName());
        assertEquals("ANONYMIZED", saved.getLastName());
        assertEquals("ANONYMIZED", saved.getPhoneNumber());
        assertNotNull(saved.getDeletedAt());
        assertNotEquals(keycloakId, saved.getKeycloakId());
        verify(cache).evict(keycloakId);
    }

    @Test
    void deleteClientShouldThrowProfileInactiveExceptionWhenAlreadyDeleted() {
        final var id = UUID.randomUUID();
        final var keycloakId = UUID.randomUUID().toString();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), keycloakId)
                .set(Select.field(Client::getDeletedAt), LocalDateTime.now())
                .create();

        when(repository.findById(id)).thenReturn(Optional.of(client));

        assertThrows(ProfileInactiveException.class, () -> clientService.deleteClient(id, keycloakId));
    }

    @Test
    void deleteClientShouldThrowClientNotFoundExceptionOnOwnershipMismatch() {
        final var id = UUID.randomUUID();
        final var client = Instancio.of(Client.class)
                .set(Select.field(Client::getKeycloakId), "actual-owner")
                .set(Select.field(Client::getDeletedAt), null)
                .create();

        when(repository.findById(id)).thenReturn(Optional.of(client));

        assertThrows(ClientNotFoundException.class,
                () -> clientService.deleteClient(id, "different-requester"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void givenClientAuthority(String role, String principalName) {
        var auth = new TestingAuthenticationToken(principalName, null,
                List.of(new SimpleGrantedAuthority(role)));
        var ctx = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
