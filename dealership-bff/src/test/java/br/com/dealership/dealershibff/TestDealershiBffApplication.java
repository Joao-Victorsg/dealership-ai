package br.com.dealership.dealershibff;

import org.springframework.boot.SpringApplication;

public class TestDealershiBffApplication {

    public static void main(String[] args) {
        SpringApplication.from(DealershiBffApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
