package br.com.dealership.clientapi.client;

import br.com.dealership.clientapi.client.dto.ViaCepResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ViaCepClient {

    private final ViaCepFeignClient feignClient;

    @CircuitBreaker(name = "viacep", fallbackMethod = "lookupPostcodeFallback")
    public Optional<ViaCepResponse> lookupPostcode(final String postcode) {
        final var response = feignClient.lookupPostcode(postcode);
        if (Boolean.TRUE.equals(response.erro())) {
            return Optional.empty();
        }
        return Optional.of(response);
    }

    @SuppressWarnings("unused")
    public Optional<ViaCepResponse> lookupPostcodeFallback(final String postcode, final Throwable t) {
        return Optional.empty();
    }
}
