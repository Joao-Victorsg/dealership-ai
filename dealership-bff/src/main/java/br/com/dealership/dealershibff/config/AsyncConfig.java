package br.com.dealership.dealershibff.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Virtual-thread executor for CompletableFuture compositions.
     * Wraps each submitted task in an MDC-propagating decorator so that
     * requestId (and any other MDC context) is available inside parallel calls.
     * (Fixes H3: MDC context propagation to CompletableFuture threads.)
     */
    @Bean
    public Executor virtualThreadExecutor() {
        final var factory = Thread.ofVirtual().factory();
        return task -> {
            final Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            factory.newThread(() -> {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                try {
                    task.run();
                } finally {
                    MDC.clear();
                }
            }).start();
        };
    }
}
