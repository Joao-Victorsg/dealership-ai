package br.com.dealership.dealershibff.feign.keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakFeignConfig {

    @Bean
    public KeycloakErrorDecoder keycloakErrorDecoder() {
        return new KeycloakErrorDecoder();
    }
}
