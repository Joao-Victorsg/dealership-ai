package br.com.dealership.dealershibff.feign.sales;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalesApiFeignConfig {

    @Bean
    public ErrorDecoder salesApiErrorDecoder() {
        return new SalesApiErrorDecoder();
    }
}
