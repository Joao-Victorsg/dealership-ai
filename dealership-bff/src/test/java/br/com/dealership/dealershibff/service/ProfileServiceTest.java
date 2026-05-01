package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import br.com.dealership.dealershibff.dto.request.UpdateProfileRequest;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ClientApiClient clientApiClient;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldReturnProfileWithEmailFromJwtOnGetProfile() throws Exception {
        final var client = Instancio.create(ClientApiClientResponse.class);
        when(clientApiClient.getMe(any())).thenReturn(client);

        final var result = profileService.getProfile("Bearer token", "user@test.com").get();

        assertNotNull(result);
        assertEquals(client.id(), result.id());
        assertEquals("user@test.com", result.email());
    }

    @Test
    void shouldReturnUpdatedProfileOnUpdateProfile() throws Exception {
        final var id = UUID.randomUUID();
        final var updated = Instancio.create(ClientApiClientResponse.class);
        when(clientApiClient.update(eq(id), any())).thenReturn(updated);

        final var request = new UpdateProfileRequest("NewName", "NewLast", "11999887766", "01310100");
        final var result = profileService.updateProfile(id, request, "user@test.com").get();

        assertNotNull(result);
        assertEquals(updated.id(), result.id());
        assertEquals("user@test.com", result.email());
    }

    @Test
    void shouldPropagateNotFoundExceptionOnGetProfile() {
        when(clientApiClient.getMe(any())).thenThrow(new NotFoundException("Not found"));

        final var future = profileService.getProfile("Bearer token", "user@test.com");
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(NotFoundException.class, ex.getCause().getClass());
    }

    @Test
    void shouldPropagateDownstreamServiceExceptionOnGetProfile() {
        when(clientApiClient.getMe(any())).thenThrow(new DownstreamServiceException("unavailable"));

        final var future = profileService.getProfile("Bearer token", "user@test.com");
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
    }
}
