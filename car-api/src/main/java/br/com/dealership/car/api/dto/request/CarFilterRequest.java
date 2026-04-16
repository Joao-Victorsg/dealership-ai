package br.com.dealership.car.api.dto.request;

import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import br.com.dealership.car.api.domain.enums.CarSortField;
import br.com.dealership.car.api.domain.enums.SortDirection;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CarFilterRequest(
        CarStatus status,
        CarCategory category,
        PropulsionType propulsionType,
        String manufacturer,
        Boolean isNew,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer minYear,
        Integer maxYear,
        CarSortField sortBy,
        SortDirection sortDirection
) {

    public CarFilterRequest {
        sortBy = sortBy != null ? sortBy : CarSortField.REGISTRATION_DATE;
        sortDirection = sortDirection != null ? sortDirection : SortDirection.DESC;

        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException("Minimum value cannot exceed maximum value");
        }
        if (minYear != null && maxYear != null && minYear > maxYear) {
            throw new IllegalArgumentException("Minimum year cannot exceed maximum year");
        }
    }
}

