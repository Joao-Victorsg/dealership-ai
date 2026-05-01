package br.com.dealership.dealershibff.feign.car;

import feign.querymap.FieldQueryMapEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CarApiFeignConfig {

    @Bean
    public CarApiErrorDecoder carApiErrorDecoder() {
        return new CarApiErrorDecoder();
    }

    @Bean
    public FieldQueryMapEncoder fieldQueryMapEncoder() {
        return new FieldQueryMapEncoder();
    }
}
