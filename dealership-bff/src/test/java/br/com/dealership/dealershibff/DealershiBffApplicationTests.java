package br.com.dealership.dealershibff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/realms/dealership/protocol/openid-connect/certs",
        "keycloak.base-url=http://localhost:9999",
        "keycloak.realm=dealership",
        "feign.client.config.car-api.url=http://localhost:9998",
        "feign.client.config.client-api.url=http://localhost:9997",
        "feign.client.config.sales-api.url=http://localhost:9996",
        "feign.client.config.keycloak.url=http://localhost:9999"
})
class DealershiBffApplicationTests {

    // Prevents Spring Boot from doing OIDC discovery at startup
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
    }

}
