package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiCreateRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AuthService {

    private static final Authentication SYSTEM_CLIENT_PRINCIPAL =
            new AnonymousAuthenticationToken(
                    "system-client-key",
                    "system-client",
                    AuthorityUtils.createAuthorityList("ROLE_SYSTEM"));

    private final ClientApiClient clientApiClient;
    private final OAuth2AuthorizedClientManager serviceAuthorizedClientManager;
    private final Executor executor;

    public AuthService(
            final ClientApiClient clientApiClient,
            @Qualifier("serviceAuthorizedClientManager")
            final OAuth2AuthorizedClientManager serviceAuthorizedClientManager,
            @Qualifier("virtualThreadExecutor") final Executor executor) {
        this.clientApiClient = clientApiClient;
        this.serviceAuthorizedClientManager = serviceAuthorizedClientManager;
        this.executor = executor;
    }

    @CircuitBreaker(name = "client-api")
    @Retry(name = "client-api")
    @RateLimiter(name = "client-api")
    @TimeLimiter(name = "client-api")
    @Bulkhead(name = "client-api")
    public CompletableFuture<ClientApiClientResponse> register(
            final String keycloakId,
            final String firstName,
            final String lastName,
            final RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            final String systemToken = getSystemAccessToken();
            final var clientRequest = ClientApiCreateRequest.builder()
                    .keycloakId(keycloakId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .cpf(request.cpf())
                    .phoneNumber(request.phone())
                    .postcode(request.cep())
                    .streetNumber(request.streetNumber())
                    .build();
            return clientApiClient.create("Bearer " + systemToken, clientRequest).data();
        }, executor);
    }

    private String getSystemAccessToken() {
        try {
            final var authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("keycloak-system")
                    .principal(SYSTEM_CLIENT_PRINCIPAL)
                    .build();
            final var client = serviceAuthorizedClientManager.authorize(authorizeRequest);
            if (client == null || client.getAccessToken() == null || client.getAccessToken().getTokenValue().isBlank()) {
                throw new DownstreamServiceException("Unable to obtain Keycloak system token for client-api call");
            }
            return client.getAccessToken().getTokenValue();
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new DownstreamServiceException("Failed to authorize Keycloak system client", ex);
        }
    }
}
