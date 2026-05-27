package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiDataResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientApiClient clientApiClient;

    @Mock
    private OAuth2AuthorizedClientManager serviceAuthorizedClientManager;

    private AuthService authService;
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @BeforeEach
    void setUp() {
        authService = new AuthService(clientApiClient, serviceAuthorizedClientManager, DIRECT_EXECUTOR);
    }

    @Test
    void shouldCallClientApiAndReturnResponseOnRegister() throws Exception {
        final var clientResponse = Instancio.create(ClientApiClientResponse.class);
        when(serviceAuthorizedClientManager.authorize(any())).thenReturn(authorizedClient("system-token"));
        when(clientApiClient.create(eq("Bearer system-token"), any())).thenReturn(new ClientApiDataResponse<>(clientResponse));

        final var request = new RegisterRequest(
                "John",
                "Doe",
                "52998224725",
                "+55 11 99988-7766",
                "01310100",
                "123");
        final var result = authService.register(
                "kc-uuid-123", "John", "Doe", request).get();

        verify(clientApiClient).create(eq("Bearer system-token"), any());
        assertEquals(clientResponse, result);
    }

    @Test
    void shouldPropagateExceptionWhenClientApiFails() {
        when(serviceAuthorizedClientManager.authorize(any())).thenReturn(authorizedClient("system-token"));
        when(clientApiClient.create(anyString(), any()))
                .thenThrow(new DownstreamServiceException("client api down"));

        final var request = new RegisterRequest(
                "John",
                "Doe",
                "52998224725",
                "+55 11 99988-7766",
                "01310100",
                "123");
        final var future = authService.register(
                "kc-uuid-123", "John", "Doe", request);

        final var ex = assertThrows(ExecutionException.class, future::get);
        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
    }

    @Test
    void shouldFailWhenSystemTokenCannotBeObtained() {
        when(serviceAuthorizedClientManager.authorize(any())).thenReturn(null);

        final var request = new RegisterRequest(
                "John",
                "Doe",
                "52998224725",
                "+55 11 99988-7766",
                "01310100",
                "123");
        final var future = authService.register("kc-uuid-123", "John", "Doe", request);

        final var ex = assertThrows(ExecutionException.class, future::get);
        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
        verify(clientApiClient, never()).create(anyString(), any());
    }

    private OAuth2AuthorizedClient authorizedClient(final String tokenValue) {
        final var registration = ClientRegistration.withRegistrationId("keycloak-system")
                .tokenUri("http://keycloak:8080/realms/dealership/protocol/openid-connect/token")
                .clientId("dealership-system")
                .clientSecret("dealership-system-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        final var accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(300));
        return new OAuth2AuthorizedClient(registration, "system-client", accessToken);
    }
}
