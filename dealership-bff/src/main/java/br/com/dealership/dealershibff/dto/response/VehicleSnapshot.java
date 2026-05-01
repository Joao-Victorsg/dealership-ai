package br.com.dealership.dealershibff.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record VehicleSnapshot(
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
