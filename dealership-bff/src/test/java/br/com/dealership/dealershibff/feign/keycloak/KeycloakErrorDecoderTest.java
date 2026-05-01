package br.com.dealership.dealershibff.feign.keycloak;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;
import br.com.dealership.dealershibff.domain.exception.BffException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.DuplicateIdentityException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class KeycloakErrorDecoderTest {

    private KeycloakErrorDecoder decoder;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        decoder = new KeycloakErrorDecoder();
        dummyRequest = Request.create(
                Request.HttpMethod.POST, "http://test/realms/dealership/protocol/openid-connect/token",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    void shouldReturnBffExceptionFor401() {
        final var response = Response.builder()
                .status(401).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("login", response);

        assertInstanceOf(BffException.class, ex);
    }

    @Test
    void shouldReturnDuplicateIdentityExceptionFor409() {
        final var response = Response.builder()
                .status(409).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("register", response);

        assertInstanceOf(DuplicateIdentityException.class, ex);
    }

    @Test
    void shouldReturnDownstreamServiceExceptionFor5xx() {
        final var response = Response.builder()
                .status(503).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("login", response);

        assertInstanceOf(DownstreamServiceException.class, ex);
    }

    @Test
    void shouldDelegateToDefaultDecoderForNon5xxDefault() {
        final var response = Response.builder()
                .status(400).headers(Collections.emptyMap()).request(dummyRequest).build();

        final var ex = decoder.decode("login", response);

        assertInstanceOf(Exception.class, ex);
    }
}
