package br.com.dealership.salesapi.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Resilience4jConfigTest {

    private Resilience4jConfig config;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        config = new Resilience4jConfig();

        logAppender = new ListAppender<>();
        logAppender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(Resilience4jConfig.class);
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(Resilience4jConfig.class);
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CircuitBreakerRegistry cbRegistry() {
        RegistryEventConsumer<CircuitBreaker> consumer = config.circuitBreakerEventConsumer();
        return CircuitBreakerRegistry.custom()
                .addRegistryEventConsumer(consumer)
                .build();
    }

    private RetryRegistry retryRegistry(RetryConfig retryConfig) {
        RegistryEventConsumer<Retry> consumer = config.retryEventConsumer();
        return RetryRegistry.custom()
                .addRegistryEventConsumer(consumer)
                .withRetryConfig(retryConfig)
                .build();
    }

    private RetryConfig noWaitRetryConfig(int maxAttempts) {
        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ZERO)
                .retryExceptions(RuntimeException.class)
                .build();
    }

    // ── Circuit Breaker ───────────────────────────────────────────────────────

    @Test
    void circuitBreaker_stateTransitionClosedToOpen_isLogged() {
        CircuitBreakerRegistry registry = cbRegistry();
        CircuitBreaker cb = registry.circuitBreaker("sns"); // triggers onEntryAdded

        cb.transitionToOpenState();

        assertEquals(1, logAppender.list.size());
        ILoggingEvent event = logAppender.list.get(0);
        assertEquals(Level.INFO, event.getLevel());
        String args = event.getArgumentArray()[0].toString();
        assertTrue(args.contains("CIRCUIT_BREAKER_STATE_CHANGED"));
        assertTrue(args.contains("sns"));
        assertTrue(args.contains("CLOSED"));
        assertTrue(args.contains("OPEN"));
    }

    @Test
    void circuitBreaker_stateTransitionOpenToHalfOpen_isLogged() {
        CircuitBreakerRegistry registry = cbRegistry();
        CircuitBreaker cb = registry.circuitBreaker("sns");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        assertEquals(2, logAppender.list.size());
        String secondArgs = logAppender.list.get(1).getArgumentArray()[0].toString();
        assertTrue(secondArgs.contains("OPEN"));
        assertTrue(secondArgs.contains("HALF_OPEN"));
    }

    @Test
    void circuitBreaker_entryRemoved_isLogged() {
        CircuitBreakerRegistry registry = cbRegistry();
        registry.circuitBreaker("sns");
        logAppender.list.clear(); // discard the onEntryAdded noise

        registry.remove("sns"); // triggers onEntryRemoved

        assertEquals(1, logAppender.list.size());
        String args = logAppender.list.get(0).getArgumentArray()[0].toString();
        assertTrue(args.contains("CIRCUIT_BREAKER_REMOVED"));
        assertTrue(args.contains("sns"));
    }

    @Test
    void circuitBreaker_entryReplaced_doesNotLog() {
        CircuitBreakerRegistry registry = cbRegistry();
        registry.circuitBreaker("sns");
        logAppender.list.clear();

        registry.replace("sns", CircuitBreaker.ofDefaults("sns")); // triggers onEntryReplaced

        assertTrue(logAppender.list.isEmpty());
    }

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Test
    void retry_retryAttempt_isLogged() {
        RetryRegistry registry = retryRegistry(noWaitRetryConfig(2));
        Retry retry = registry.retry("sns"); // triggers onEntryAdded

        Supplier<String> failing = Retry.decorateSupplier(retry, () -> {
            throw new RuntimeException("sns down");
        });
        try { failing.get(); } catch (Exception ignored) { }

        // maxAttempts=2 → 1 RETRY_ATTEMPT + 1 RETRY_EXHAUSTED
        assertEquals(2, logAppender.list.size());
        String args = logAppender.list.get(0).getArgumentArray()[0].toString();
        assertTrue(args.contains("RETRY_ATTEMPT"));
        assertTrue(args.contains("sns"));
        assertTrue(args.contains("RuntimeException"));
    }

    @Test
    void retry_retryExhausted_isLogged() {
        RetryRegistry registry = retryRegistry(noWaitRetryConfig(2));
        Retry retry = registry.retry("sns");

        Supplier<String> failing = Retry.decorateSupplier(retry, () -> {
            throw new RuntimeException("sns down");
        });
        try { failing.get(); } catch (Exception ignored) { }

        String exhaustedArgs = logAppender.list.getLast().getArgumentArray()[0].toString();
        assertTrue(exhaustedArgs.contains("RETRY_EXHAUSTED"));
        assertTrue(exhaustedArgs.contains("sns"));
        assertTrue(exhaustedArgs.contains("2"));
    }

    @Test
    void retry_retryExhausted_withNullMessage_doesNotThrow() {
        RetryRegistry registry = retryRegistry(noWaitRetryConfig(2));
        Retry retry = registry.retry("sns");

        // RuntimeException() has a null message — exercises the getMessage() null path
        Supplier<String> failing = Retry.decorateSupplier(retry, () -> {
            throw new RuntimeException();
        });
        try { failing.get(); } catch (Exception ignored) { }

        String exhaustedArgs = logAppender.list.getLast().getArgumentArray()[0].toString();
        assertTrue(exhaustedArgs.contains("RETRY_EXHAUSTED"));
    }

    @Test
    void retry_entryRemoved_isLogged() {
        RetryRegistry registry = retryRegistry(noWaitRetryConfig(3));
        registry.retry("sns");
        logAppender.list.clear();

        registry.remove("sns"); // triggers onEntryRemoved

        assertEquals(1, logAppender.list.size());
        String args = logAppender.list.get(0).getArgumentArray()[0].toString();
        assertTrue(args.contains("RETRY_REMOVED"));
        assertTrue(args.contains("sns"));
    }

    @Test
    void retry_entryReplaced_doesNotLog() {
        RetryRegistry registry = retryRegistry(noWaitRetryConfig(3));
        registry.retry("sns");
        logAppender.list.clear();

        registry.replace("sns", Retry.ofDefaults("sns")); // triggers onEntryReplaced

        assertTrue(logAppender.list.isEmpty());
    }
}
