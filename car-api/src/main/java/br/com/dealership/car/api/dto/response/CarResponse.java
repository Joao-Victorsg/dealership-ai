package br.com.dealership.car.api.dto.response;

import br.com.dealership.car.api.domain.entity.Car;
import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Car inventory item representation")
public record CarResponse(
        @Schema(description = "Unique identifier") UUID id,
        @Schema(description = "Car model name", example = "Civic") String model,
        @Schema(description = "Manufacturing year", example = "2024") Integer manufacturingYear,
        @Schema(description = "Manufacturer name", example = "Honda") String manufacturer,
        @Schema(description = "Exterior color", example = "Pearl White") String externalColor,
        @Schema(description = "Interior color", example = "Black") String internalColor,
        @Schema(description = "Vehicle Identification Number", example = "1HGBH41JXMN109186") String vin,
        @Schema(description = "Current inventory status") CarStatus status,
        @Schema(description = "Optional features/items") List<String> optionalItems,
        @Schema(description = "Vehicle category") CarCategory category,
        @Schema(description = "Odometer reading in km", example = "0") BigDecimal kilometers,
        @Schema(description = "Whether the car is brand-new") Boolean isNew,
        @Schema(description = "Engine/propulsion type") PropulsionType propulsionType,
        @Schema(description = "Listed sale price", example = "45000.00") BigDecimal listedValue,
        @Schema(description = "S3 object key for the car image") String imageKey,
        @Schema(description = "Timestamp when the car was registered") Instant registrationDate
) {

    public CarResponse {
        optionalItems = optionalItems != null ? List.copyOf(optionalItems) : List.of();
    }

    public static CarResponse from(Car car) {
        return CarResponse.builder()
                .id(car.getId())
                .model(car.getModel())
                .manufacturingYear(car.getManufacturingYear())
                .manufacturer(car.getManufacturer())
                .externalColor(car.getExternalColor())
                .internalColor(car.getInternalColor())
                .vin(car.getVin())
                .status(car.getStatus())
                .optionalItems(car.getOptionalItems())
                .category(car.getCategory())
                .kilometers(car.getKilometers())
                .isNew(car.getIsNew())
                .propulsionType(car.getPropulsionType())
                .listedValue(car.getListedValue())
                .imageKey(car.getImageKey())
                .registrationDate(car.getRegistrationDate())
                .build();
    }
}
