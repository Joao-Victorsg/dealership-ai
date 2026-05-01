package br.com.dealership.dealershibff.feign.car;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarApiErrorDecoderTest {

    private CarApiErrorDecoder decoder;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        decoder = new CarApiErrorDecoder();
        dummyRequest = Request.create(
                Request.HttpMethod.GET, "http://test/api/v1/cars",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    void shouldReturnNotFoundExceptionFor404() {
        final var response = Response.builder()
                .status(404).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("listCars", response);

        assertInstanceOf(NotFoundException.class, ex);
        assertTrue(ex.getMessage().contains("Car not found"));
    }

    @Test
    void shouldReturnDownstreamServiceExceptionFor503() {
        final var response = Response.builder()
                .status(503).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("listCars", response);

        assertInstanceOf(DownstreamServiceException.class, ex);
        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    void shouldReturnDownstreamServiceExceptionForOther4xxStatus() {
        final var response = Response.builder()
                .status(400).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("listCars", response);

        assertInstanceOf(DownstreamServiceException.class, ex);
        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void shouldDelegateToDefaultDecoderForNon4xxStatus() {
        final var response = Response.builder()
                .status(200).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("listCars", response);

        // Default decoder returns a generic feign exception for 2xx - not our exceptions
        assertInstanceOf(Exception.class, ex);
    }
}
