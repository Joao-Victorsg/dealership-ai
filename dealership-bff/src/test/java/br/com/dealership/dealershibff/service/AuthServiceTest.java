package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientApiClient clientApiClient;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldCallClientApiAndReturnResponseOnRegister() throws Exception {
        final var clientResponse = Instancio.create(ClientApiClientResponse.class);
        when(clientApiClient.create(anyString(), any())).thenReturn(clientResponse);

        final var request = new RegisterRequest("52998224725", "+55 11 99988-7766", "01310100", "123");
        final var result = authService.register(
                "kc-uuid-123", "John", "Doe", "access-token-value", request).get();

        verify(clientApiClient).create(anyString(), any());
        assertEquals(clientResponse, result);
    }

    @Test
    void shouldPropagateExceptionWhenClientApiFails() {
        when(clientApiClient.create(anyString(), any()))
                .thenThrow(new DownstreamServiceException("client api down"));

        final var request = new RegisterRequest("52998224725", "+55 11 99988-7766", "01310100", "123");
        final var future = authService.register(
                "kc-uuid-123", "John", "Doe", "access-token-value", request);

        final var ex = assertThrows(ExecutionException.class, future::get);
        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
    }
}
