package br.com.dealership.dealershibff.feign.sales.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record SalesApiCarSnapshot(
        String model,
        String manufacturer,
        String externalColor,
        String internalColor,
        Integer manufacturingYear,
        List<String> optionalItems,
        String type,
        String category,
        String vin,
        BigDecimal listedValue,
        String status
) {
}
