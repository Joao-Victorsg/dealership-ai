package br.com.dealership.car.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class S3ObjectDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectDeletionListener.class);

    private final S3Service s3Service;

    public S3ObjectDeletionListener(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeletion(S3ObjectDeletionEvent event) {
        log.info("Deleting S3 object after transaction commit: {}", event.objectKey());
        s3Service.deleteObject(event.objectKey());
    }
}
