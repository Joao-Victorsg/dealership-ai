package br.com.dealership.car.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignedUrlRequest(
        @NotBlank
        @Pattern(
                regexp = "image/jpeg|image/png|image/webp",
                message = "Content type must be one of: image/jpeg, image/png, image/webp"
        )
        String contentType
) {

    public static PresignedUrlRequest of(String contentType) {
        return new PresignedUrlRequest(contentType);
    }
}
