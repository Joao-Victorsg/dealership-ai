package br.com.dealership.salesapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SalesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesApiApplication.class, args);
    }

}
