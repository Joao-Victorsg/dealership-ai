package br.com.dealership.dealershibff.feign.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientApiFeignConfig {

    @Bean
    public ClientApiErrorDecoder clientApiErrorDecoder() {
        return new ClientApiErrorDecoder();
    }
}
