package br.com.dealership.dealershibff.feign.car.dto;

import java.math.BigDecimal;

public record CarApiFilterParams(
        String q,
        String category,
        String type,
        String condition,
        String manufacturer,
        Integer yearMin,
        Integer yearMax,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String externalColor,
        BigDecimal kmMin,
        BigDecimal kmMax,
        String status,
        String propulsionType,
        Boolean isNew,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer minYear,
        Integer maxYear,
        String sortBy,
        String sortDirection,
        Integer page,
        Integer size
) {
}
