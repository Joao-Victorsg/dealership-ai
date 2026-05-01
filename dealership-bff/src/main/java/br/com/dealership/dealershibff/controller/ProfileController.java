package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.dto.request.UpdateProfileRequest;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.ProfileResponse;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import br.com.dealership.dealershibff.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/profile")
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Profile", description = "Client profile view and update")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(final ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's profile")
    public CompletableFuture<ResponseEntity<ApiResponse<ProfileResponse>>> getProfile(
            final JwtAuthenticationToken authentication) {
        final String bearerToken = "Bearer " + authentication.getToken().getTokenValue();
        final String email = authentication.getToken().getClaimAsString("email");
        return profileService.getProfile(bearerToken, email)
                .thenApply(profile -> ResponseEntity.ok(
                        ApiResponse.of(profile, ResponseMeta.of(getRequestId()))));
    }

    @PatchMapping
    @Operation(summary = "Update the authenticated user's profile")
    public CompletableFuture<ResponseEntity<ApiResponse<ProfileResponse>>> updateProfile(
            @RequestBody @Valid final UpdateProfileRequest request,
            final JwtAuthenticationToken authentication) {
        final String email = authentication.getToken().getClaimAsString("email");
        final UUID clientId = UUID.fromString(authentication.getToken().getSubject());
        return profileService.updateProfile(clientId, request, email)
                .thenApply(profile -> ResponseEntity.ok(
                        ApiResponse.of(profile, ResponseMeta.of(getRequestId()))));
    }

    private String getRequestId() {
        final String mdcValue = MDC.get("requestId");
        return mdcValue != null ? mdcValue : java.util.UUID.randomUUID().toString();
    }
}
