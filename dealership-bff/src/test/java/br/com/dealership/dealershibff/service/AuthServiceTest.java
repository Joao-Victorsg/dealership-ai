package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.DuplicateIdentityException;
import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.keycloak.KeycloakClient;
import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakTokenResponse;
import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakUserResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private ClientApiClient clientApiClient;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldCallClientApiAfterKeycloakUserCreatedOnRegister() throws Exception {
        final var adminToken = Instancio.create(KeycloakTokenResponse.class);
        final var clientResponse = Instancio.create(ClientApiClientResponse.class);
        when(keycloakClient.login(any(MultiValueMap.class))).thenReturn(adminToken);
        when(keycloakClient.searchUsers(anyString(), anyString(), any(Boolean.class)))
                .thenReturn(List.of(new KeycloakUserResponse("kc-uuid-123", "user@test.com", "user@test.com")));
        when(clientApiClient.create(anyString(), any())).thenReturn(clientResponse);

        final var request = new RegisterRequest(
                "user@test.com", "password123",
                "John", "Doe", "52998224725", "+55 11 99988-7766", "01310100", "123");

        final var result = authService.register(request).get();

        verify(clientApiClient).create(anyString(), any());
        verify(keycloakClient).sendVerifyEmail(anyString(), eq("kc-uuid-123"));
        assertEquals(clientResponse, result);
    }

    @Test
    void shouldCallKeycloakDeleteWhenClientApiFails() throws Exception {
        final var adminToken = Instancio.create(KeycloakTokenResponse.class);
        when(keycloakClient.login(any(MultiValueMap.class))).thenReturn(adminToken);
        when(keycloakClient.searchUsers(anyString(), anyString(), any(Boolean.class)))
                .thenReturn(List.of(new KeycloakUserResponse("kc-uuid-123", "user@test.com", "user@test.com")));
        when(clientApiClient.create(anyString(), any())).thenThrow(new DownstreamServiceException("client api down"));

        final var request = new RegisterRequest(
                "user@test.com", "password123",
                "John", "Doe", "52998224725", "+55 11 99988-7766", "01310100", "123");

        final var future = authService.register(request);
        assertThrows(ExecutionException.class, future::get);

        verify(keycloakClient).deleteUser(anyString(), anyString());
    }

    @Test
    void shouldPropagateDuplicateIdentityExceptionOnRegister() {
        when(keycloakClient.login(any(MultiValueMap.class)))
                .thenReturn(Instancio.create(KeycloakTokenResponse.class));
        doThrow(new DuplicateIdentityException("Email already registered"))
                .when(keycloakClient).createUser(anyString(), any());

        final var request = new RegisterRequest(
                "existing@test.com", "password123",
                "John", "Doe", "52998224725", "+55 11 99988-7766", "01310100", "123");

        final var future = authService.register(request);
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(DuplicateIdentityException.class, ex.getCause().getClass());
        verify(clientApiClient, never()).create(anyString(), any());
    }
}
