package br.com.dealership.salesapi.dto.response;

import br.com.dealership.salesapi.domain.entity.CarSnapshot;
import br.com.dealership.salesapi.domain.entity.CarStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CarSnapshotResponse(
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
        CarStatus status
) {
    public static CarSnapshotResponse from(CarSnapshot snapshot) {
        return CarSnapshotResponse.builder()
                .model(snapshot.model())
                .manufacturer(snapshot.manufacturer())
                .externalColor(snapshot.externalColor())
                .internalColor(snapshot.internalColor())
                .manufacturingYear(snapshot.manufacturingYear())
                .optionalItems(snapshot.optionalItems())
                .type(snapshot.type())
                .category(snapshot.category())
                .vin(snapshot.vin())
                .listedValue(snapshot.listedValue())
                .status(snapshot.status())
                .build();
    }
}
