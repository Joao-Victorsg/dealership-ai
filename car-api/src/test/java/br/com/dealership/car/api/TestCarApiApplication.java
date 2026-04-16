package br.com.dealership.car.api;

import org.springframework.boot.SpringApplication;

public class TestCarApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(CarApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
