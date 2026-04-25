package br.com.dealership.salesapi.controller;

import br.com.dealership.salesapi.dto.request.RegisterSaleRequest;
import br.com.dealership.salesapi.dto.request.StaffSaleFilterRequest;
import br.com.dealership.salesapi.dto.response.Response;
import br.com.dealership.salesapi.dto.response.SaleResponse;
import br.com.dealership.salesapi.service.SaleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales")
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Response<SaleResponse>> registerSale(
            @Valid @RequestBody RegisterSaleRequest request,
            JwtAuthenticationToken token,
            UriComponentsBuilder uriBuilder) {
        final var saleResponse = saleService.registerSale(request, token);
        final var location = uriBuilder.path("/api/v1/sales/{id}")
                .buildAndExpand(saleResponse.id())
                .toUri();
        return ResponseEntity.created(location).body(Response.of(saleResponse));
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Response<Page<SaleResponse>>> getClientSales(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable,
            JwtAuthenticationToken token) {
        final var clientId = UUID.fromString(token.getName());
        final var page = saleService.getClientSales(clientId, from, to, pageable);
        return ResponseEntity.ok(Response.of(page));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<Response<Page<SaleResponse>>> getStaffSales(
            @ModelAttribute StaffSaleFilterRequest filter,
            Pageable pageable) {
        final var page = saleService.getStaffSales(filter, pageable);
        return ResponseEntity.ok(Response.of(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','STAFF','ADMIN')")
    public ResponseEntity<Response<SaleResponse>> getById(
            @PathVariable UUID id,
            JwtAuthenticationToken token) {
        final var isStaff = token.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        final var saleResponse = saleService.getById(id, token, isStaff);
        return ResponseEntity.ok(Response.of(saleResponse));
    }
}
