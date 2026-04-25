package br.com.dealership.salesapi.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ErrorResponse(String message, List<FieldError> errors) {
}
