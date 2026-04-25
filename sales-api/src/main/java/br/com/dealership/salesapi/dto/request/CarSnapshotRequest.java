package br.com.dealership.salesapi.dto.request;

import br.com.dealership.salesapi.domain.entity.CarStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record CarSnapshotRequest(
        @NotBlank String model,
        @NotBlank String manufacturer,
        @NotBlank String externalColor,
        @NotBlank String internalColor,
        @NotNull @Min(1886) @Max(2100) Integer manufacturingYear,
        List<String> optionalItems,
        @NotBlank String type,
        @NotBlank String category,
        @NotBlank @Pattern(regexp = "[A-Z0-9]{17}") String vin,
        @NotNull @DecimalMin("0.01") BigDecimal listedValue,
        @NotNull CarStatus status
) {
    public CarSnapshotRequest {
        optionalItems = optionalItems != null ? List.copyOf(optionalItems) : List.of();
        if (vin != null) {
            vin = vin.toUpperCase();
        }
    }
}
