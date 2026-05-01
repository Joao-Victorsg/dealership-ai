package br.com.dealership.dealershibff.feign.sales.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record SalesApiVehicleSnapshot(
        UUID id,
        String model,
        String manufacturer,
        Integer manufacturingYear,
        String externalColor,
        String vin,
        String category,
        BigDecimal listedValue
) {
}
