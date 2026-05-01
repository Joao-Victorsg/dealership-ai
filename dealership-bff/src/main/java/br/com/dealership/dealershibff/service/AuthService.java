package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiCreateRequest;
import br.com.dealership.dealershibff.feign.keycloak.KeycloakClient;
import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakCreateUserRequest;
import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakTokenResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.concurrent.CompletableFuture;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final KeycloakClient keycloakClient;
    private final ClientApiClient clientApiClient;

    @Value("${keycloak.system-client-id:dealership-system}")
    private String systemClientId;

    @Value("${keycloak.system-client-secret}")
    private String systemClientSecret;

    public AuthService(final KeycloakClient keycloakClient, final ClientApiClient clientApiClient) {
        this.keycloakClient = keycloakClient;
        this.clientApiClient = clientApiClient;
    }

    @CircuitBreaker(name = "keycloak")
    @Retry(name = "keycloak")
    @RateLimiter(name = "keycloak")
    @TimeLimiter(name = "keycloak")
    @Bulkhead(name = "keycloak")
    public CompletableFuture<ClientApiClientResponse> register(final RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            final var createUserReq = KeycloakCreateUserRequest.of(request.email(), request.password());
            final var adminToken = obtainAdminToken();
            keycloakClient.createUser(adminToken, createUserReq);

            final var keycloakUserId = extractKeycloakUserId(request.email(), adminToken);

            try {
                keycloakClient.sendVerifyEmail(adminToken, keycloakUserId);
            } catch (Exception e) {
                log.warn("Could not send verification email [keycloakId={} requestId={}]",
                        keycloakUserId, MDC.get("requestId"));
            }

            try {
                final var clientRequest = ClientApiCreateRequest.builder()
                        .keycloakId(keycloakUserId)
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .cpf(request.cpf())
                        .phoneNumber(request.phone())
                        .postcode(request.cep())
                        .streetNumber(request.streetNumber())
                        .build();
                return clientApiClient.create(adminToken, clientRequest);
            } catch (Exception e) {
                final String requestId = MDC.get("requestId");
                log.warn("Client API failed after Keycloak user creation [keycloakId={} requestId={}] — compensating",
                        keycloakUserId, requestId);
                try {
                    keycloakClient.deleteUser(adminToken, keycloakUserId);
                } catch (Exception compensationEx) {
                    log.error("Registration compensation failed; orphaned Keycloak user {} requestId={}",
                            keycloakUserId, requestId, compensationEx);
                }
                throw e;
            }
        });
    }

    private String obtainAdminToken() {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", systemClientId);
        form.add("client_secret", systemClientSecret);
        return "Bearer " + keycloakClient.login(form).accessToken();
    }

    private String extractKeycloakUserId(final String email, final String adminToken) {
        final var users = keycloakClient.searchUsers(adminToken, email, true);
        if (users == null || users.isEmpty()) {
            throw new IllegalStateException("Keycloak user not found after creation: " + email);
        }
        return users.get(0).id();
    }
}
