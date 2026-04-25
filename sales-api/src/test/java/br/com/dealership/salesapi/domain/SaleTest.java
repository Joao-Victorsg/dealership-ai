package br.com.dealership.salesapi.domain;

import br.com.dealership.salesapi.domain.entity.CarStatus;
import br.com.dealership.salesapi.domain.entity.CarSnapshot;
import br.com.dealership.salesapi.dto.request.CarSnapshotRequest;
import br.com.dealership.salesapi.dto.request.RegisterSaleRequest;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleTest {

    @Test
    void carSnapshotRequestNormalizesVinToUppercase() {
        var request = Instancio.of(CarSnapshotRequest.class)
                .set(Select.field(CarSnapshotRequest::vin), "abc12345678901234")
                .set(Select.field(CarSnapshotRequest::status), CarStatus.AVAILABLE)
                .set(Select.field(CarSnapshotRequest::listedValue), BigDecimal.valueOf(50000))
                .set(Select.field(CarSnapshotRequest::manufacturingYear), 2020)
                .set(Select.field(CarSnapshotRequest::optionalItems), List.of())
                .create();

        assertEquals("ABC12345678901234", request.vin());
    }

    @Test
    void carSnapshotRequestDefensivelyCopiesOptionalItems() {
        var mutableList = new ArrayList<>(Arrays.asList("Sunroof", "GPS"));
        var request = Instancio.of(CarSnapshotRequest.class)
                .set(Select.field(CarSnapshotRequest::vin), "ABC12345678901234")
                .set(Select.field(CarSnapshotRequest::status), CarStatus.AVAILABLE)
                .set(Select.field(CarSnapshotRequest::listedValue), BigDecimal.valueOf(50000))
                .set(Select.field(CarSnapshotRequest::manufacturingYear), 2020)
                .set(Select.field(CarSnapshotRequest::optionalItems), mutableList)
                .create();

        mutableList.add("Leather");
        assertEquals(2, request.optionalItems().size());
        assertThrows(UnsupportedOperationException.class,
                () -> request.optionalItems().add("NewItem"));
    }

    @Test
    void registerSaleRequestThrowsWhenCarIdEqualsClientId() {
        UUID sameId = UUID.randomUUID();
        var base = Instancio.of(RegisterSaleRequest.class).create();
        assertThrows(IllegalArgumentException.class,
                () -> new RegisterSaleRequest(sameId, sameId,
                        base.clientSnapshot(), base.carSnapshot()));
    }

    @Test
    void carSnapshotEntityDefensivelyCopiesOptionalItems() {
        var mutableList = new ArrayList<>(Arrays.asList("Sunroof"));
        var snapshot = Instancio.of(CarSnapshot.class)
                .set(Select.field(CarSnapshot::optionalItems), mutableList)
                .create();

        mutableList.add("GPS");
        assertNotSame(mutableList, snapshot.optionalItems());
        assertEquals(1, snapshot.optionalItems().size());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.optionalItems().add("NewItem"));
    }

    @Test
    void carSnapshotRequestWithNullOptionalItemsDefaultsToEmptyList() {
        var request = new CarSnapshotRequest(
                "Model", "Brand", "Red", "Black", 2020,
                null, "Sedan", "Premium", "ABC12345678901234",
                BigDecimal.valueOf(50000), CarStatus.AVAILABLE);

        assertTrue(request.optionalItems().isEmpty());
    }

    @Test
    void carSnapshotRequestWithNullVinLeavesVinNull() {
        var request = new CarSnapshotRequest(
                "Model", "Brand", "Red", "Black", 2020,
                List.of(), "Sedan", "Premium", null,
                BigDecimal.valueOf(50000), CarStatus.AVAILABLE);

        assertEquals(null, request.vin());
    }
}
