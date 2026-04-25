package br.com.dealership.salesapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterSaleRequest(
        @NotNull UUID carId,
        @NotNull UUID clientId,
        @Valid @NotNull ClientSnapshotRequest clientSnapshot,
        @Valid @NotNull CarSnapshotRequest carSnapshot
) {
    public RegisterSaleRequest {
        if (carId != null && clientId != null && carId.equals(clientId)) {
            throw new IllegalArgumentException("carId and clientId must not be equal");
        }
    }
}
