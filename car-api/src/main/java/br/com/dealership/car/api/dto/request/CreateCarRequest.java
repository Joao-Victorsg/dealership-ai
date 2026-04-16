package br.com.dealership.car.api.dto.request;

import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Builder
@Schema(description = "Request payload for registering a new car in inventory")
public record CreateCarRequest(
        @NotBlank @Size(max = 255) @Schema(description = "Car model name", example = "Civic") String model,
        @NotNull @Schema(description = "Year the car was manufactured", example = "2024") Integer manufacturingYear,
        @NotBlank @Size(max = 255) @Schema(description = "Car manufacturer", example = "Honda") String manufacturer,
        @NotBlank @Size(max = 100) @Schema(description = "Exterior color", example = "Pearl White") String externalColor,
        @NotBlank @Size(max = 100) @Schema(description = "Interior color", example = "Black") String internalColor,
        @NotBlank
        @Pattern(regexp = "[A-Z0-9]{17}", message = "VIN must be exactly 17 alphanumeric characters")
        @Schema(description = "Vehicle Identification Number (17 chars, auto-uppercased)", example = "1HGBH41JXMN109186")
        String vin,
        @NotNull @Schema(description = "Initial status (cannot be SOLD)") CarStatus status,
        @Schema(description = "List of optional items/features") List<String> optionalItems,
        @NotNull @Schema(description = "Vehicle category") CarCategory category,
        @NotNull @DecimalMin("0") @Schema(description = "Odometer reading in km", example = "0") BigDecimal kilometers,
        @NotNull @Schema(description = "Whether this is a brand-new car") Boolean isNew,
        @NotNull @Schema(description = "Engine/propulsion type") PropulsionType propulsionType,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "Listed value must be a positive number")
        @Schema(description = "Sale price", example = "45000.00") BigDecimal listedValue,
        @Schema(description = "S3 object key for car image") String imageKey
) {

    private static final int MIN_MANUFACTURING_YEAR = 1886;

    public CreateCarRequest {
        vin = normalizeVin(vin);
        validateManufacturingYear(manufacturingYear);
        validateKilometersConsistency(isNew, kilometers);
        validateStatus(status);
        imageKey = normalizeImageKey(imageKey);
        optionalItems = normalizeOptionalItems(optionalItems);
    }

    private static String normalizeVin(String vin) {
        return vin != null ? vin.toUpperCase(Locale.ROOT) : null;
    }

    private static void validateManufacturingYear(Integer manufacturingYear) {
        if (manufacturingYear == null) return;
        int maxYear = Year.now().getValue() + 1;
        if (manufacturingYear < MIN_MANUFACTURING_YEAR || manufacturingYear > maxYear) {
            throw new IllegalArgumentException(
                    "Manufacturing year must be between %d and %d".formatted(MIN_MANUFACTURING_YEAR, maxYear));
        }
    }

    private static void validateKilometersConsistency(Boolean isNew, BigDecimal kilometers) {
        if (isNew == null || kilometers == null) return;
        if (isNew && kilometers.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("A new car must have zero kilometers");
        }
        if (!isNew && kilometers.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A used car must have kilometers greater than zero");
        }
    }

    private static void validateStatus(CarStatus status) {
        if (CarStatus.SOLD == status) {
            throw new IllegalArgumentException("Initial status cannot be Sold");
        }
    }

    private static String normalizeImageKey(String imageKey) {
        return (imageKey != null && imageKey.isBlank()) ? null : imageKey;
    }

    private static List<String> normalizeOptionalItems(List<String> optionalItems) {
        return optionalItems != null ? List.copyOf(optionalItems) : List.of();
    }
}
