package br.com.dealership.dealershibff.feign.client;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.DuplicateIdentityException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientApiErrorDecoderTest {

    private ClientApiErrorDecoder decoder;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        decoder = new ClientApiErrorDecoder();
        dummyRequest = Request.create(
                Request.HttpMethod.GET, "http://test/api/v1/clients/me",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    void shouldReturnNotFoundExceptionFor404() {
        final var response = Response.builder()
                .status(404).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("getMe", response);

        assertInstanceOf(NotFoundException.class, ex);
    }

    @Test
    void shouldReturnDuplicateIdentityExceptionFor409() {
        final var response = Response.builder()
                .status(409).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("createClient", response);

        assertInstanceOf(DuplicateIdentityException.class, ex);
    }

    @Test
    void shouldReturnDownstreamServiceExceptionFor5xx() {
        final var response = Response.builder()
                .status(500).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("getMe", response);

        assertInstanceOf(DownstreamServiceException.class, ex);
        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    void shouldDelegateToDefaultDecoderForNon5xxDefaultStatus() {
        final var response = Response.builder()
                .status(400).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("getMe", response);

        assertInstanceOf(Exception.class, ex);
    }
}
