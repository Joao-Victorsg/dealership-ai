package br.com.dealership.clientapi.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class ViaCepClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ViaCepClient client;

    @BeforeEach
    void setUp() {
        final var feignClient = Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(new JacksonDecoder())
                .target(ViaCepFeignClient.class, wm.baseUrl());
        client = new ViaCepClient(feignClient);
    }

    @Test
    void shouldReturnPopulatedResponseOnSuccessfulLookup() {
        wm.stubFor(get(urlEqualTo("/ws/01310100/json/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"cep":"01310-100","logradouro":"Avenida Paulista","localidade":"São Paulo","uf":"SP"}
                                """)));

        final var result = client.lookupPostcode("01310100");

        assertTrue(result.isPresent());
        assertEquals("São Paulo", result.get().localidade());
        assertEquals("SP", result.get().uf());
        assertEquals("Avenida Paulista", result.get().logradouro());
    }

    @Test
    void shouldReturnEmptyWhenViaCepRespondsWithErroTrue() {
        wm.stubFor(get(urlEqualTo("/ws/99999999/json/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"erro\": true}")));

        final var result = client.lookupPostcode("99999999");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenViaCepReturns503() {
        wm.stubFor(get(urlEqualTo("/ws/00000000/json/"))
                .willReturn(aResponse().withStatus(503)));

        // Feign throws on non-2xx; @CircuitBreaker AOP proxy catches this in production
        // and routes to lookupPostcodeFallback, which returns Optional.empty()
        assertThrows(feign.FeignException.class, () -> client.lookupPostcode("00000000"));
    }

    @Test
    void fallbackMethodShouldReturnEmpty() {
        final var result = client.lookupPostcodeFallback("12345678", new RuntimeException("circuit open"));

        assertTrue(result.isEmpty());
    }
}
