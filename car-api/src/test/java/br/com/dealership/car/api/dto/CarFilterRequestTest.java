package br.com.dealership.car.api.dto;

import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarSortField;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import br.com.dealership.car.api.domain.enums.SortDirection;
import br.com.dealership.car.api.dto.request.CarFilterRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarFilterRequestTest {

    @Test
    void shouldDefaultSortByToRegistrationDateWhenNotProvided() {
        var filter = CarFilterRequest.builder().build();
        assertEquals(CarSortField.REGISTRATION_DATE, filter.sortBy());
    }

    @Test
    void shouldDefaultSortDirectionToDescWhenNotProvided() {
        var filter = CarFilterRequest.builder().build();
        assertEquals(SortDirection.DESC, filter.sortDirection());
    }

    @Test
    void shouldKeepProvidedSortByField() {
        var filter = CarFilterRequest.builder().sortBy(CarSortField.LISTED_VALUE).build();
        assertEquals(CarSortField.LISTED_VALUE, filter.sortBy());
    }

    @Test
    void shouldKeepProvidedSortDirection() {
        var filter = CarFilterRequest.builder().sortDirection(SortDirection.ASC).build();
        assertEquals(SortDirection.ASC, filter.sortDirection());
    }

    @Test
    void shouldThrowWhenMinValueExceedsMaxValue() {
        assertThrows(IllegalArgumentException.class, () ->
                CarFilterRequest.builder()
                        .minValue(BigDecimal.valueOf(100000))
                        .maxValue(BigDecimal.valueOf(50000))
                        .build());
    }

    @Test
    void shouldThrowWhenMinYearExceedsMaxYear() {
        assertThrows(IllegalArgumentException.class, () ->
                CarFilterRequest.builder()
                        .minYear(2025)
                        .maxYear(2020)
                        .build());
    }

    @Test
    void shouldAcceptWhenMinValueEqualsMaxValue() {
        var filter = CarFilterRequest.builder()
                .minValue(BigDecimal.valueOf(50000))
                .maxValue(BigDecimal.valueOf(50000))
                .build();
        assertEquals(BigDecimal.valueOf(50000), filter.minValue());
    }

    @Test
    void shouldAcceptWhenMinYearEqualsMaxYear() {
        var filter = CarFilterRequest.builder()
                .minYear(2022)
                .maxYear(2022)
                .build();
        assertEquals(2022, filter.minYear());
    }

    @Test
    void shouldAcceptValidFilterWithAllFields() {
        var filter = CarFilterRequest.builder()
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .propulsionType(PropulsionType.ELECTRIC)
                .manufacturer("Tesla")
                .isNew(false)
                .minValue(BigDecimal.valueOf(30000))
                .maxValue(BigDecimal.valueOf(80000))
                .minYear(2020)
                .maxYear(2024)
                .sortBy(CarSortField.LISTED_VALUE)
                .sortDirection(SortDirection.ASC)
                .build();

        assertEquals(CarStatus.AVAILABLE, filter.status());
        assertEquals(CarCategory.SEDAN, filter.category());
        assertEquals("Tesla", filter.manufacturer());
    }

    @Test
    void shouldAcceptFilterWithOnlyMinValue() {
        var filter = CarFilterRequest.builder()
                .minValue(BigDecimal.valueOf(20000))
                .build();
        assertEquals(BigDecimal.valueOf(20000), filter.minValue());
    }

    @Test
    void shouldAcceptFilterWithOnlyMaxValue() {
        var filter = CarFilterRequest.builder()
                .maxValue(BigDecimal.valueOf(90000))
                .build();
        assertEquals(BigDecimal.valueOf(90000), filter.maxValue());
    }

    @Test
    void shouldAcceptFilterWithOnlyMinYear() {
        var filter = CarFilterRequest.builder().minYear(2020).build();
        assertEquals(2020, filter.minYear());
    }

    @Test
    void shouldAcceptFilterWithOnlyMaxYear() {
        var filter = CarFilterRequest.builder().maxYear(2024).build();
        assertEquals(2024, filter.maxYear());
    }
}


