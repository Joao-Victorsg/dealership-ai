package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.dto.request.UpdateProfileRequest;
import br.com.dealership.dealershibff.dto.response.ProfileResponse;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiUpdateRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProfileService {

    private final ClientApiClient clientApiClient;

    public ProfileService(final ClientApiClient clientApiClient) {
        this.clientApiClient = clientApiClient;
    }

    @CircuitBreaker(name = "client-api")
    @Retry(name = "client-api")
    @RateLimiter(name = "client-api")
    @TimeLimiter(name = "client-api")
    @Bulkhead(name = "client-api")
    public CompletableFuture<ProfileResponse> getProfile(final String bearerToken, final String emailFromJwt) {
        return CompletableFuture.supplyAsync(() -> {
            final var client = clientApiClient.getMe(bearerToken);
            return ProfileResponse.from(client, emailFromJwt);
        });
    }

    @CircuitBreaker(name = "client-api")
    @Retry(name = "client-api")
    @RateLimiter(name = "client-api")
    @TimeLimiter(name = "client-api")
    @Bulkhead(name = "client-api")
    public CompletableFuture<ProfileResponse> updateProfile(
            final UUID clientId,
            final UpdateProfileRequest request,
            final String emailFromJwt) {
        return CompletableFuture.supplyAsync(() -> {
            final var updateRequest = new ClientApiUpdateRequest(
                    request.firstName(),
                    request.lastName(),
                    request.phone(),
                    request.cep()
            );
            final var updated = clientApiClient.update(clientId, updateRequest);
            return ProfileResponse.from(updated, emailFromJwt);
        });
    }
}
