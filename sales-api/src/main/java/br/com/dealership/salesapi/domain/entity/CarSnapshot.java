package br.com.dealership.salesapi.domain.entity;

import br.com.dealership.salesapi.dto.request.CarSnapshotRequest;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CarSnapshot(
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
    public CarSnapshot {
        optionalItems = optionalItems != null ? List.copyOf(optionalItems) : List.of();
    }

    public static CarSnapshot from(CarSnapshotRequest r) {
        return CarSnapshot.builder()
                .model(r.model())
                .manufacturer(r.manufacturer())
                .externalColor(r.externalColor())
                .internalColor(r.internalColor())
                .manufacturingYear(r.manufacturingYear())
                .optionalItems(r.optionalItems())
                .type(r.type())
                .category(r.category())
                .vin(r.vin())
                .listedValue(r.listedValue())
                .status(r.status())
                .build();
    }
}
