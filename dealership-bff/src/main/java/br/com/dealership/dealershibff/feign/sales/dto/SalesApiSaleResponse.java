package br.com.dealership.dealershibff.feign.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SalesApiSaleResponse(
        UUID id,
        UUID carId,
        UUID clientId,
        BigDecimal saleValue,
        Instant registeredAt,
        SalesApiClientSnapshot clientSnapshot,
        SalesApiCarSnapshot carSnapshot,
        String status,
        SalesApiVehicleSnapshot vehicle,
        SalesApiClientSnapshot client
) {
}
