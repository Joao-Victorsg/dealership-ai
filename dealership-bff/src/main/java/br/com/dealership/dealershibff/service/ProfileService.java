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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ProfileService {

    private final ClientApiClient clientApiClient;
    private final Executor executor;

    public ProfileService(
            final ClientApiClient clientApiClient,
            @Qualifier("virtualThreadExecutor") final Executor executor) {
        this.clientApiClient = clientApiClient;
        this.executor = executor;
    }

    @CircuitBreaker(name = "client-api")
    @Retry(name = "client-api")
    @RateLimiter(name = "client-api")
    @TimeLimiter(name = "client-api")
    @Bulkhead(name = "client-api")
    public CompletableFuture<ProfileResponse> getProfile(final String bearerToken, final String emailFromJwt) {
        return CompletableFuture.supplyAsync(() -> {
            final var client = clientApiClient.getMe(bearerToken).data();
            return ProfileResponse.from(client, emailFromJwt);
        }, executor);
    }

    @CircuitBreaker(name = "client-api")
    @Retry(name = "client-api")
    @RateLimiter(name = "client-api")
    @TimeLimiter(name = "client-api")
    @Bulkhead(name = "client-api")
    public CompletableFuture<ProfileResponse> updateProfile(
            final String bearerToken,
            final UpdateProfileRequest request,
            final String emailFromJwt) {
        return CompletableFuture.supplyAsync(() -> {
            final var current = clientApiClient.getMe(bearerToken).data();
            final var updateRequest = new ClientApiUpdateRequest(
                    request.firstName(),
                    request.lastName(),
                    request.phone(),
                    request.cep(),
                    null
            );
            final var updated = clientApiClient.update(current.id(), bearerToken, updateRequest).data();
            return ProfileResponse.from(updated, emailFromJwt);
        }, executor);
    }
}
