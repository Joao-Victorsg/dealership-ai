package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.dto.request.PurchaseRequest;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.PurchaseResponse;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import br.com.dealership.dealershibff.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/purchases")
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Purchases", description = "Purchase initiation and history")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(final PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @Operation(summary = "Initiate a car purchase")
    public CompletableFuture<ResponseEntity<ApiResponse<PurchaseResponse>>> purchase(
            @RequestBody @Valid final PurchaseRequest request,
            final JwtAuthenticationToken authentication) {
        final String bearerToken = "Bearer " + authentication.getToken().getTokenValue();
        final String email = authentication.getToken().getClaimAsString("email");
        return purchaseService.purchase(request.carId(), bearerToken, email)
                .thenApply(purchase -> ResponseEntity.status(201)
                        .body(ApiResponse.of(purchase, ResponseMeta.of(getRequestId()))));
    }

    @GetMapping
    @Operation(summary = "Retrieve purchase history")
    public CompletableFuture<ResponseEntity<ApiResponse<List<PurchaseResponse>>>> history(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(required = false) final Instant from,
            @RequestParam(required = false) final Instant to,
            final JwtAuthenticationToken authentication) {
        final String bearerToken = "Bearer " + authentication.getToken().getTokenValue();
        final String requestId = getRequestId();
        return purchaseService.history(bearerToken, page, size, from, to, requestId)
                .thenApply(ResponseEntity::ok);
    }

    private String getRequestId() {
        final String mdcValue = MDC.get("requestId");
        return mdcValue != null ? mdcValue : UUID.randomUUID().toString();
    }
}
