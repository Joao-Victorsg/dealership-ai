package br.com.dealership.dealershibff.config;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import org.slf4j.MDC;
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
            final Token token = NewRelic.getAgent().getTransaction().getToken();
            factory.newThread(() -> {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                try {
                    if (token != null) {
                        token.link();
                    }
                    task.run();
                } finally {
                    try {
                        if (token != null) {
                            token.expire();
                        }
                    } finally {
                        MDC.clear();
                    }
                }
            }).start();
        };
    }
}
