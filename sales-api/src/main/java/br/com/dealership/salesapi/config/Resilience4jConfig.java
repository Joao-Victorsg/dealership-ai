package br.com.dealership.salesapi.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
public class Resilience4jConfig {

    private static final String KEY_EVENT_TYPE = "event_type";
    private static final String KEY_INSTANCE   = "instance";
    private static final String KEY_TIMESTAMP  = "timestamp";

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {

            @Override
            public void onEntryAddedEvent(@Nonnull EntryAddedEvent<CircuitBreaker> event) {
                event.getAddedEntry().getEventPublisher()
                        .onStateTransition(e -> logCircuitBreakerStateChange(
                                e.getCircuitBreakerName(),
                                e.getStateTransition().getFromState().toString(),
                                e.getStateTransition().getToState().toString()
                        ));
            }

            @Override
            public void onEntryRemovedEvent(@Nonnull EntryRemovedEvent<CircuitBreaker> event) {
                logCircuitBreakerRemoved(event.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(@Nonnull EntryReplacedEvent<CircuitBreaker> event) {
                // replacement is not a meaningful lifecycle event for logging
            }
        };
    }

    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<>() {

            @Override
            public void onEntryAddedEvent(@Nonnull EntryAddedEvent<Retry> event) {
                event.getAddedEntry().getEventPublisher()
                        .onRetry(e -> {
                            var throwable = e.getLastThrowable();
                            logRetryAttempt(
                                    e.getName(),
                                    e.getNumberOfRetryAttempts(),
                                    throwable != null ? throwable.getClass().getSimpleName() : "unknown"
                            );
                        })
                        .onError(e -> {
                            var throwable = e.getLastThrowable();
                            logRetryExhausted(
                                    e.getName(),
                                    e.getNumberOfRetryAttempts(),
                                    throwable != null ? throwable.getClass().getSimpleName() : "unknown",
                                    throwable != null ? throwable.getMessage() : null
                            );
                        });
            }

            @Override
            public void onEntryRemovedEvent(@Nonnull EntryRemovedEvent<Retry> event) {
                logRetryRemoved(event.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(@Nonnull EntryReplacedEvent<Retry> event) {
                // replacement is not a meaningful lifecycle event for logging
            }
        };
    }

    private void logCircuitBreakerStateChange(String instance, String fromState, String toState) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(KEY_EVENT_TYPE, "CIRCUIT_BREAKER_STATE_CHANGED");
        fields.put(KEY_INSTANCE, instance);
        fields.put("from_state", fromState);
        fields.put("to_state", toState);
        fields.put(KEY_TIMESTAMP, Instant.now().toString());
        writeLog(fields);
    }

    private void logCircuitBreakerRemoved(String instance) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(KEY_EVENT_TYPE, "CIRCUIT_BREAKER_REMOVED");
        fields.put(KEY_INSTANCE, instance);
        fields.put(KEY_TIMESTAMP, Instant.now().toString());
        writeLog(fields);
    }

    private void logRetryAttempt(String instance, int attemptNumber, String exceptionType) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(KEY_EVENT_TYPE, "RETRY_ATTEMPT");
        fields.put(KEY_INSTANCE, instance);
        fields.put("attempt_number", attemptNumber);
        fields.put("exception_type", exceptionType);
        fields.put(KEY_TIMESTAMP, Instant.now().toString());
        writeLog(fields);
    }

    private void logRetryExhausted(String instance, int attempts, String exceptionType, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(KEY_EVENT_TYPE, "RETRY_EXHAUSTED");
        fields.put(KEY_INSTANCE, instance);
        fields.put("total_attempts", attempts);
        fields.put("exception_type", exceptionType);
        fields.put("exception_message", message);
        fields.put(KEY_TIMESTAMP, Instant.now().toString());
        writeLog(fields);
    }

    private void logRetryRemoved(String instance) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(KEY_EVENT_TYPE, "RETRY_REMOVED");
        fields.put(KEY_INSTANCE, instance);
        fields.put(KEY_TIMESTAMP, Instant.now().toString());
        writeLog(fields);
    }

    /**
     * Logs structured fields as top-level JSON attributes via StructuredArguments.entries().
     * LogstashEncoder merges the map entries directly into the JSON log line, so New Relic
     * can index each field (event_type, instance, etc.) as a searchable attribute.
     */
    private void writeLog(Map<String, Object> fields) {
        log.info("resilience4j event", StructuredArguments.entries(fields));
    }
}
