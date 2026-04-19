package br.com.dealership.clientapi.controller;

import br.com.dealership.clientapi.dto.request.CreateClientRequest;
import br.com.dealership.clientapi.dto.request.UpdateClientRequest;
import br.com.dealership.clientapi.dto.request.UpdateCpfRequest;
import br.com.dealership.clientapi.dto.response.ClientResponse;
import br.com.dealership.clientapi.dto.response.Response;
import br.com.dealership.clientapi.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/clients")
@Tag(name = "Clients", description = "Customer profile management")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Register a new client profile", operationId = "createClient",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Profile created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "422", description = "Duplicate CPF or Keycloak ID")
    public ResponseEntity<Response<ClientResponse>> createClient(@Valid @RequestBody CreateClientRequest request) {
        final ClientResponse response = clientService.createClient(request);
        final var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(Response.of(response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get own client profile", operationId = "getMyProfile",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Profile returned")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden or profile not found")
    public ResponseEntity<Response<ClientResponse>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Response.of(clientService.getMyProfile(jwt.getSubject())));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT') or hasRole('SYSTEM')")
    @Operation(summary = "Update client profile (personal fields and/or address)", operationId = "updateClient",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden or profile not found")
    @ApiResponse(responseCode = "422", description = "Profile is inactive")
    public ResponseEntity<Response<ClientResponse>> updateClient(@PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateClientRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Response.of(clientService.updateClient(id, request, jwt.getSubject())));
    }

    @PatchMapping("/{id}/cpf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Correct client CPF (admin only)", operationId = "correctCpf", tags = {"Admin"},
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "CPF corrected")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden or profile not found")
    @ApiResponse(responseCode = "422", description = "Duplicate CPF")
    public ResponseEntity<Response<ClientResponse>> correctCpf(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateCpfRequest request) {
        return ResponseEntity.ok(Response.of(clientService.correctCpf(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Delete (anonymize) client account", operationId = "deleteClient",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Account anonymized")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden or profile not found")
    @ApiResponse(responseCode = "422", description = "Profile already inactive")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id,
                                             @AuthenticationPrincipal Jwt jwt) {
        clientService.deleteClient(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
