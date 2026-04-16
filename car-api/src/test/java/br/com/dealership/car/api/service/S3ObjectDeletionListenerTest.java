package br.com.dealership.car.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ObjectDeletionListenerTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private S3ObjectDeletionListener listener;

    @Test
    void shouldDelegateToS3ServiceWhenHandlingDeletion() {
        var event = new S3ObjectDeletionEvent("cars/some-car/image.jpg");

        listener.handleDeletion(event);

        verify(s3Service).deleteObject("cars/some-car/image.jpg");
    }

    @Test
    void shouldHandleDeletionWithAnyObjectKey() {
        var event = new S3ObjectDeletionEvent("cars/uuid-123/uuid-456.png");

        listener.handleDeletion(event);

        verify(s3Service).deleteObject("cars/uuid-123/uuid-456.png");
    }
}

