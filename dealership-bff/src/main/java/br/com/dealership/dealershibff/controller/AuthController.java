package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.dto.request.RegisterRequest;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import br.com.dealership.dealershibff.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "User authentication and registration")
public class AuthController {

    private final AuthService authService;

    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Complete business registration for an authenticated user")
    public CompletableFuture<ResponseEntity<ApiResponse<ClientApiClientResponse>>> register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid final RegisterRequest request) {
        final String keycloakId = jwt.getSubject();
        final String firstName = jwt.getClaimAsString("given_name");
        final String lastName = jwt.getClaimAsString("family_name");
        final String bearerToken = jwt.getTokenValue();
        return authService.register(keycloakId, firstName, lastName, bearerToken, request)
                .thenApply(client -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.of(client, ResponseMeta.of(getRequestId()))));
    }

    private String getRequestId() {
        final String mdcValue = MDC.get("requestId");
        return mdcValue != null ? mdcValue : UUID.randomUUID().toString();
    }
}
