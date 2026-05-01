package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.dto.request.InventoryFilterRequest;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.VehicleResponse;
import br.com.dealership.dealershibff.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Public car inventory browsing and search")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(final InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List cars with optional filters, sorting, and pagination")
    public CompletableFuture<ResponseEntity<ApiResponse<List<VehicleResponse>>>> list(
            @ModelAttribute final InventoryFilterRequest filter) {
        return inventoryService.list(filter)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single car by its ID")
    public CompletableFuture<ResponseEntity<ApiResponse<VehicleResponse>>> getById(
            @PathVariable final UUID id) {
        return inventoryService.getById(id)
                .thenApply(ResponseEntity::ok);
    }
}
