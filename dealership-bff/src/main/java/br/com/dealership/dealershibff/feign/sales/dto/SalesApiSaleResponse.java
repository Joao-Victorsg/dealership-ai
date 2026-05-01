package br.com.dealership.dealershibff.feign.sales.dto;

import java.time.Instant;
import java.util.UUID;

public record SalesApiSaleResponse(
        UUID id,
        Instant registeredAt,
        String status,
        SalesApiVehicleSnapshot vehicle,
        SalesApiClientSnapshot client
) {
}
