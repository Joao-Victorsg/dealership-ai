package br.com.dealership.dealershibff.feign.sales;

import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SalesApiErrorDecoderTest {

    private SalesApiErrorDecoder decoder;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        decoder = new SalesApiErrorDecoder();
        dummyRequest = Request.create(
                Request.HttpMethod.POST, "http://test/api/v1/sales",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    void shouldReturnCarNotAvailableExceptionFor409() {
        final var response = Response.builder()
                .status(409).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("registerSale", response);

        assertInstanceOf(CarNotAvailableException.class, ex);
    }

    @Test
    void shouldReturnDownstreamServiceExceptionForOtherStatuses() {
        final var response = Response.builder()
                .status(500).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("registerSale", response);

        assertInstanceOf(DownstreamServiceException.class, ex);
    }
}
