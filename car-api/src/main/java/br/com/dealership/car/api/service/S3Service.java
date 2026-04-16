package br.com.dealership.car.api.service;

import br.com.dealership.car.api.config.S3Properties;
import br.com.dealership.car.api.dto.response.PresignedUrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public PresignedUrlResponse generatePresignedPutUrl(UUID carId, String contentType) {
        var extension = resolveExtension(contentType);
        var objectKey = "cars/" + carId + "/" + UUID.randomUUID() + "." + extension;

        var presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(put -> put.bucket(properties.bucket())
                        .key(objectKey)
                        .contentType(contentType))
                .signatureDuration(Duration.ofSeconds(properties.presignedUrlTtl()))
                .build();

        var presigned = s3Presigner.presignPutObject(presignRequest);
        return PresignedUrlResponse.of(presigned.url().toString(), objectKey, properties.presignedUrlTtl());
    }

    public void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object '{}': {}", objectKey, e.getMessage());
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}
