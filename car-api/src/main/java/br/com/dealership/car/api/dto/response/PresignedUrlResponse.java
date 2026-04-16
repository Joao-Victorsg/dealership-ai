package br.com.dealership.car.api.dto.response;

public record PresignedUrlResponse(
        String presignedUrl,
        String objectKey,
        int expiresIn
) {

    public static PresignedUrlResponse of(String presignedUrl, String objectKey, int expiresIn) {
        return new PresignedUrlResponse(presignedUrl, objectKey, expiresIn);
    }
}
