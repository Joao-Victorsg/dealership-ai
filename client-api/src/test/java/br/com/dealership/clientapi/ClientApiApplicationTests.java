package br.com.dealership.clientapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ClientApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
