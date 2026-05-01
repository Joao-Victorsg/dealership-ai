package br.com.dealership.dealershibff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "br.com.dealership.dealershibff.feign")
public class DealershiBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealershiBffApplication.class, args);
    }

}
