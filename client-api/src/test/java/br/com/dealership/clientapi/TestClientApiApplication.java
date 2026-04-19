package br.com.dealership.clientapi;

import org.springframework.boot.SpringApplication;

public class TestClientApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ClientApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
