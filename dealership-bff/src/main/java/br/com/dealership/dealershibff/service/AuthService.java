package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiCreateRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AuthService {

    private final ClientApiClient clientApiClient;

    public AuthService(final ClientApiClient clientApiClient) {
        this.clientApiClient = clientApiClient;
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
            final String bearerToken,
            final RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            final var clientRequest = ClientApiCreateRequest.builder()
                    .keycloakId(keycloakId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .cpf(request.cpf())
                    .phoneNumber(request.phone())
                    .postcode(request.cep())
                    .streetNumber(request.streetNumber())
                    .build();
            return clientApiClient.create("Bearer " + bearerToken, clientRequest);
        });
    }
}
