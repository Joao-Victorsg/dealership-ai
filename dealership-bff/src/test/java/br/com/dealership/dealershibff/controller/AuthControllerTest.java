package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.config.GlobalExceptionHandler;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        setUpSecurityContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void setUpSecurityContext() {
        final Jwt testJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("kc-id")
                .claim("given_name", "John")
                .claim("family_name", "Doe")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(testJwt, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @Test
    void shouldReturn201OnRegisterSuccess() throws Exception {
        final var clientResponse = new ClientApiClientResponse(
                UUID.randomUUID(), "kc-id", "John", "Doe", "***", "+55 11 99988-7766",
                Instant.now(), null, null);
        when(authService.register(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(clientResponse));

        final var mvcResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"52998224725",
                                  "phone":"+55 (11) 99988-7766",
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
    void shouldReturn400OnInvalidCpfForRegister() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf":"00000000000",
                                  "phone":"+55 (11) 99988-7766",
                                  "cep":"01310100",
                                  "streetNumber":"123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("cpf"));
    }
}
