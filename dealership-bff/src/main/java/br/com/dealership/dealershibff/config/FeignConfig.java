package br.com.dealership.dealershibff.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global Feign configuration. Must NOT be placed inside a Feign client sub-package,
 * as that would cause it to be component-scanned exclusively by that client.
 * Per-client error decoders and request interceptors are registered via
 * per-client @Configuration classes (e.g., CarApiFeignConfig, ClientApiFeignConfig).
 */
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
