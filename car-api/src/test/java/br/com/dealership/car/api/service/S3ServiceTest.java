package br.com.dealership.car.api.service;

import br.com.dealership.car.api.config.S3Properties;
import br.com.dealership.car.api.dto.response.PresignedUrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Properties properties;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void shouldGeneratePresignedUrlForJpegContentType() throws Exception {
        var carId = UUID.randomUUID();
        when(properties.bucket()).thenReturn("test-bucket");
        when(properties.presignedUrlTtl()).thenReturn(3600);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/cars/" + carId + "/image.jpg").toURL());

        var result = s3Service.generatePresignedPutUrl(carId, "image/jpeg");

        assertNotNull(result);
        assertTrue(result.objectKey().endsWith(".jpg"));
        assertTrue(result.objectKey().startsWith("cars/" + carId + "/"));
        assertEquals(3600, result.expiresIn());
    }

    @Test
    void shouldGeneratePresignedUrlForPngContentType() throws Exception {
        var carId = UUID.randomUUID();
        when(properties.bucket()).thenReturn("test-bucket");
        when(properties.presignedUrlTtl()).thenReturn(900);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/cars/image.png").toURL());

        var result = s3Service.generatePresignedPutUrl(carId, "image/png");

        assertTrue(result.objectKey().endsWith(".png"));
        assertEquals(900, result.expiresIn());
    }

    @Test
    void shouldGeneratePresignedUrlForWebpContentType() throws Exception {
        var carId = UUID.randomUUID();
        when(properties.bucket()).thenReturn("test-bucket");
        when(properties.presignedUrlTtl()).thenReturn(3600);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/cars/image.webp").toURL());

        var result = s3Service.generatePresignedPutUrl(carId, "image/webp");

        assertTrue(result.objectKey().endsWith(".webp"));
    }

    @Test
    void shouldGeneratePresignedUrlWithBinExtensionForUnknownContentType() throws Exception {
        var carId = UUID.randomUUID();
        when(properties.bucket()).thenReturn("test-bucket");
        when(properties.presignedUrlTtl()).thenReturn(3600);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/cars/image.bin").toURL());

        var result = s3Service.generatePresignedPutUrl(carId, "application/octet-stream");

        assertTrue(result.objectKey().endsWith(".bin"));
    }

    @Test
    void shouldDeleteObjectSuccessfully() {
        var objectKey = "cars/test-car/image.jpg";

        assertDoesNotThrow(() -> s3Service.deleteObject(objectKey));

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void shouldSwallowExceptionWhenDeletionFails() {
        var objectKey = "cars/test-car/image.jpg";
        doThrow(new RuntimeException("S3 deletion failed")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertDoesNotThrow(() -> s3Service.deleteObject(objectKey));
    }
}


