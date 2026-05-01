package br.com.dealership.dealershibff.feign.sales;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class SalesApiFeignConfig {

    @Bean
    public ErrorDecoder salesApiErrorDecoder() {
        return new SalesApiErrorDecoder();
    }
}
