package br.com.dealership.dealershibff.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PurchaseRequest(
        @NotNull UUID carId
) {
}
