package br.com.dealership.car.api.dto;

import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.dto.request.UpdateCarRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateCarRequestTest {

    @Test
    void shouldThrowWhenAllFieldsAreNull() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateCarRequest(null, null, null));
    }

    @Test
    void shouldAcceptWhenOnlyStatusIsProvided() {
        var request = UpdateCarRequest.of(CarStatus.SOLD, null, null);
        assertEquals(CarStatus.SOLD, request.status());
    }

    @Test
    void shouldAcceptWhenOnlyListedValueIsProvided() {
        var request = UpdateCarRequest.of(null, BigDecimal.valueOf(60000), null);
        assertEquals(BigDecimal.valueOf(60000), request.listedValue());
    }

    @Test
    void shouldAcceptWhenOnlyImageKeyIsProvided() {
        var request = UpdateCarRequest.of(null, null, "cars/image.jpg");
        assertEquals("cars/image.jpg", request.imageKey());
    }

    @Test
    void shouldAcceptWhenAllFieldsAreProvided() {
        var request = UpdateCarRequest.of(CarStatus.UNAVAILABLE, BigDecimal.valueOf(45000), "cars/image.jpg");
        assertEquals(CarStatus.UNAVAILABLE, request.status());
        assertEquals(BigDecimal.valueOf(45000), request.listedValue());
        assertEquals("cars/image.jpg", request.imageKey());
    }

    @Test
    void shouldAcceptBlankImageKeyAsValidValue() {
        var request = UpdateCarRequest.of(null, null, "  ");
        assertEquals("  ", request.imageKey());
    }
}

