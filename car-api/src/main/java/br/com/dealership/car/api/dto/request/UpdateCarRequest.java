package br.com.dealership.car.api.dto.request;

import br.com.dealership.car.api.domain.enums.CarStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Schema(description = "Request payload for partially updating a car's mutable attributes")
public record UpdateCarRequest(
        @Schema(description = "New status for the car") CarStatus status,
        @DecimalMin(value = "0", inclusive = false, message = "Listed value must be a positive number")
        @Schema(description = "New listed sale price", example = "42000.00") BigDecimal listedValue,
        @Schema(description = "New S3 object key for car image (blank to remove)") String imageKey
) {

    public UpdateCarRequest {
        if (status == null && listedValue == null && imageKey == null) {
            throw new IllegalArgumentException("No fields to update");
        }
    }

    public static UpdateCarRequest of(CarStatus status, BigDecimal listedValue, String imageKey) {
        return new UpdateCarRequest(status, listedValue, imageKey);
    }
}
