package br.com.dealership.car.api.dto;

import br.com.dealership.car.api.dto.request.CreateCarRequest;
import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateCarRequestTest {

    private static final String VALID_VIN = "ABCDE1234567890AB";

    private CreateCarRequest.CreateCarRequestBuilder validBuilder() {
        return CreateCarRequest.builder()
                .model("Tesla Model 3")
                .manufacturingYear(2020)
                .manufacturer("Tesla")
                .externalColor("White")
                .internalColor("Black")
                .vin(VALID_VIN)
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.ELECTRIC)
                .listedValue(BigDecimal.valueOf(50000));
    }

    @Test
    void shouldNormalizeVinToUppercase() {
        var request = validBuilder()
                .vin("abcde1234567890ab")
                .build();

        assertEquals("ABCDE1234567890AB", request.vin());
    }

    @Test
    void shouldRejectWhenYearBelow1886() {
        assertThrows(IllegalArgumentException.class, () ->
                validBuilder().manufacturingYear(1885).build());
    }

    @Test
    void shouldRejectWhenYearAboveCurrentPlusOne() {
        assertThrows(IllegalArgumentException.class, () ->
                validBuilder().manufacturingYear(9999).build());
    }

    @Test
    void shouldRejectWhenNewCarHasKilometersAboveZero() {
        assertThrows(IllegalArgumentException.class, () ->
                validBuilder()
                        .isNew(true)
                        .kilometers(BigDecimal.valueOf(500))
                        .build());
    }

    @Test
    void shouldRejectWhenUsedCarHasZeroKilometers() {
        assertThrows(IllegalArgumentException.class, () ->
                validBuilder()
                        .isNew(false)
                        .kilometers(BigDecimal.ZERO)
                        .build());
    }

    @Test
    void shouldRejectWhenInitialStatusIsSold() {
        assertThrows(IllegalArgumentException.class, () ->
                validBuilder().status(CarStatus.SOLD).build());
    }

    @Test
    void shouldTreatEmptyImageKeyAsNull() {
        var request = validBuilder().imageKey("  ").build();

        assertNull(request.imageKey());
    }

    @Test
    void shouldTreatNullImageKeyAsNull() {
        var request = validBuilder().imageKey(null).build();

        assertNull(request.imageKey());
    }

    @Test
    void shouldDefaultOptionalItemsToEmptyList() {
        var request = validBuilder().optionalItems(null).build();

        assertEquals(0, request.optionalItems().size());
    }

    @Test
    void shouldAcceptValidRequest() {
        var request = validBuilder().build();

        assertEquals("Tesla Model 3", request.model());
        assertEquals(VALID_VIN, request.vin());
        assertEquals(CarStatus.AVAILABLE, request.status());
    }
}

