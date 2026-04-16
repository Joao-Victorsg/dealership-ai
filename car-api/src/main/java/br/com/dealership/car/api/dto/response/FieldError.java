package br.com.dealership.car.api.dto.response;

public record FieldError(
        String field,
        String message
) {

    public static FieldError of(String field, String message) {
        return new FieldError(field, message);
    }
}
