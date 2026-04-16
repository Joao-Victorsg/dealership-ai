package br.com.dealership.car.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String bucket,
        String region,
        String endpoint,
        int presignedUrlTtl
) {
}
