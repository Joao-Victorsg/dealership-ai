package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.config.GlobalExceptionHandler;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn201OnRegisterSuccess() throws Exception {
        final var clientResponse = new ClientApiClientResponse(
                UUID.randomUUID(), "kc-id", "John", "Doe", "***", "+55 11 99988-7766",
                Instant.now(), null, null);
        when(authService.register(any()))
                .thenReturn(CompletableFuture.completedFuture(clientResponse));

        final var mvcResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"user@test.com",
                                  "password":"password123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "cpf":"52998224725",
                                  "phone":"+55 11 99988-7766",
                                  "cep":"01310100",
                                  "streetNumber":"123"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldReturn400OnBlankPasswordForRegister() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"user@test.com",
                                  "password":"short",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "cpf":"52998224725",
                                  "phone":"+55 11 99988-7766",
                                  "cep":"01310100",
                                  "streetNumber":"123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("password"));
    }
}
