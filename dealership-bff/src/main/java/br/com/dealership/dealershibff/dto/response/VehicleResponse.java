package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record VehicleResponse(
        UUID id,
        String model,
        String manufacturer,
        Integer manufacturingYear,
        String externalColor,
        String internalColor,
        String vin,
        String status,
        String category,
        String type,
        Boolean isNew,
        BigDecimal kilometers,
        String propulsionType,
        BigDecimal listedValue,
        String imageKey,
        List<String> optionalItems,
        Instant registrationDate
) {

    public static VehicleResponse from(final CarApiCarResponse source) {
        return VehicleResponse.builder()
                .id(source.id())
                .model(source.model())
                .manufacturer(source.manufacturer())
                .manufacturingYear(source.manufacturingYear())
                .externalColor(source.externalColor())
                .internalColor(source.internalColor())
                .vin(source.vin())
                .status(source.status())
                .category(source.category())
                .type(source.type())
                .isNew(source.isNew())
                .kilometers(source.kilometers())
                .propulsionType(source.propulsionType())
                .listedValue(source.listedValue())
                .imageKey(source.imageKey())
                .optionalItems(source.optionalItems())
                .registrationDate(source.registrationDate())
                .build();
    }
}
