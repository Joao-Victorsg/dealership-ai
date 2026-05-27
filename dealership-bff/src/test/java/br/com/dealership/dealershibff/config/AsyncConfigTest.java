package br.com.dealership.dealershibff.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AsyncConfigTest {

    private final Executor executor = new AsyncConfig().virtualThreadExecutor();

    @Test
    void shouldPropagateMdcToSubmittedTask() throws Exception {
        MDC.put("requestId", "req-123");
        try {
            final var capturedRequestId = new CompletableFuture<String>();
            executor.execute(() -> capturedRequestId.complete(MDC.get("requestId")));
            assertEquals("req-123", capturedRequestId.get(5, TimeUnit.SECONDS));
        } finally {
            MDC.clear();
        }
    }

    @Test
    void shouldNotLeakMdcAcrossTasks() throws Exception {
        MDC.put("requestId", "req-a");
        try {
            final var firstTaskRequestId = new CompletableFuture<String>();
            executor.execute(() -> firstTaskRequestId.complete(MDC.get("requestId")));
            assertEquals("req-a", firstTaskRequestId.get(5, TimeUnit.SECONDS));
        } finally {
            MDC.clear();
        }

        final var secondTaskRequestId = new CompletableFuture<String>();
        executor.execute(() -> secondTaskRequestId.complete(MDC.get("requestId")));
        assertNull(secondTaskRequestId.get(5, TimeUnit.SECONDS));
    }
}
