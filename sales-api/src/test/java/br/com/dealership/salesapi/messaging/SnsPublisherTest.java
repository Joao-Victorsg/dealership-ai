package br.com.dealership.salesapi.messaging;

import br.com.dealership.salesapi.domain.exception.SnsPublishException;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnsPublisherTest {

    @Mock
    private SnsTemplate snsTemplate;

    @Mock
    private SnsProperties snsProperties;

    @InjectMocks
    private SnsPublisher snsPublisher;

    @Test
    void publishCallsSnsTemplateWithCorrectTopicArn() {
        String topicArn = "arn:aws:sns:us-east-1:000000000000:sale-events";
        SaleEventPayload payload = Instancio.create(SaleEventPayload.class);
        when(snsProperties.topicArn()).thenReturn(topicArn);

        snsPublisher.publish(payload);

        verify(snsTemplate).sendNotification(eq(topicArn), any(SnsNotification.class));
    }

    @Test
    void publishFallbackThrowsSnsPublishException() {
        SaleEventPayload payload = Instancio.create(SaleEventPayload.class);
        Throwable cause = new RuntimeException("SNS unreachable");

        assertThrows(SnsPublishException.class,
                () -> snsPublisher.publishFallback(payload, cause));
    }

    @Test
    void publishPropagatesExceptionFromSnsTemplate() {
        SaleEventPayload payload = Instancio.create(SaleEventPayload.class);
        when(snsProperties.topicArn()).thenReturn("arn:aws:sns:us-east-1:000000000000:sale-events");
        doThrow(new RuntimeException("SNS error")).when(snsTemplate).sendNotification(any(String.class), any(SnsNotification.class));

        assertThrows(RuntimeException.class, () -> snsPublisher.publish(payload));
    }
}

