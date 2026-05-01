package br.com.dealership.dealershibff.dto.request;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryFilterRequestTest {

    @Test
    void shouldThrowWhenPriceMinGreaterThanPriceMax() {
        assertThrows(IllegalArgumentException.class, () ->
                new InventoryFilterRequest(null, null, null, null, null,
                        null, null, BigDecimal.valueOf(10000), BigDecimal.valueOf(5000),
                        null, null, null, null, null, 0, 20));
    }

    @Test
    void shouldAllowValidPriceRange() {
        assertDoesNotThrow(() ->
                new InventoryFilterRequest(null, null, null, null, null,
                        null, null, BigDecimal.valueOf(5000), BigDecimal.valueOf(10000),
                        null, null, null, null, null, 0, 20));
    }

    @Test
    void shouldAllowPriceMinSetWithPriceMaxNull() {
        assertDoesNotThrow(() ->
                new InventoryFilterRequest(null, null, null, null, null,
                        null, null, BigDecimal.valueOf(5000), null,
                        null, null, null, null, null, 0, 20));
    }

    @Test
    void shouldThrowWhenYearMinGreaterThanYearMax() {
        assertThrows(IllegalArgumentException.class, () ->
                new InventoryFilterRequest(null, null, null, null, null,
                        2025, 2020, null, null, null, null, null, null, null, 0, 20));
    }

    @Test
    void shouldAllowValidYearRange() {
        assertDoesNotThrow(() ->
                new InventoryFilterRequest(null, null, null, null, null,
                        2020, 2025, null, null, null, null, null, null, null, 0, 20));
    }

    @Test
    void shouldAllowYearMinSetWithYearMaxNull() {
        assertDoesNotThrow(() ->
                new InventoryFilterRequest(null, null, null, null, null,
                        2020, null, null, null, null, null, null, null, null, 0, 20));
    }
}
