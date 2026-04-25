package br.com.dealership.salesapi.messaging;

import br.com.dealership.salesapi.domain.exception.SnsPublishException;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnsPublisher {

    private final SnsTemplate snsTemplate;
    private final SnsProperties snsProperties;

    @Retry(name = "sns", fallbackMethod = "publishFallback")
    @CircuitBreaker(name = "sns", fallbackMethod = "publishFallback")
    public void publish(SaleEventPayload payload) {
        snsTemplate.sendNotification(snsProperties.topicArn(), SnsNotification.of(payload));
    }

    void publishFallback(SaleEventPayload payload, Throwable t) {
        throw new SnsPublishException(t.getMessage());
    }
}
