package br.com.dealership.car.api.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {

    public ErrorResponse {
        timestamp = timestamp != null ? timestamp : Instant.now();
        fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : null;
    }
}
