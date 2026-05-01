package br.com.dealership.dealershibff.feign.car.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CarApiCarResponse(
        UUID id,
        String model,
        String manufacturer,
        Integer manufacturingYear,
        String externalColor,
        String internalColor,
        String vin,
        String status,
        String category,
        String type,
        @JsonProperty("isNew") Boolean isNew,
        BigDecimal kilometers,
        String propulsionType,
        BigDecimal listedValue,
        String imageKey,
        List<String> optionalItems,
        Instant registrationDate
) {
}
