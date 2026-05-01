package integrated;

import com.github.tomakehurst.wiremock.WireMockServer;
import integrated.utils.JwtTestUtils;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

public class EnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static final WireMockServer CAR_API_MOCK = new WireMockServer(0);
    private static final WireMockServer CLIENT_API_MOCK = new WireMockServer(0);
    private static final WireMockServer SALES_API_MOCK = new WireMockServer(0);
    private static final WireMockServer KEYCLOAK_MOCK = new WireMockServer(0);

    static {
        REDIS.start();
        CAR_API_MOCK.start();
        CLIENT_API_MOCK.start();
        SALES_API_MOCK.start();
        KEYCLOAK_MOCK.start();

        // Register JWKS endpoint on keycloak mock with test public key
        registerJwksEndpoint();
    }

    private static void registerJwksEndpoint() {
        final var publicKey = JwtTestUtils.getPublicKey();
        final String jwksJson;
        try {
            jwksJson = "{\"keys\":[" + publicKey.toJSONString() + "]}";
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test JWKS", e);
        }
        KEYCLOAK_MOCK.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                                com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo(
                                        "/realms/dealership/protocol/openid-connect/certs"))
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(jwksJson)));
    }

    public static WireMockServer getCarApiMock() {
        return CAR_API_MOCK;
    }

    public static WireMockServer getClientApiMock() {
        return CLIENT_API_MOCK;
    }

    public static WireMockServer getSalesApiMock() {
        return SALES_API_MOCK;
    }

    public static WireMockServer getKeycloakMock() {
        return KEYCLOAK_MOCK;
    }

    @Override
    public void initialize(final ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "spring.data.redis.host=" + REDIS.getHost(),
                "spring.data.redis.port=" + REDIS.getMappedPort(6379),
                "spring.cloud.openfeign.client.config.car-api.url=http://localhost:" + CAR_API_MOCK.port(),
                "spring.cloud.openfeign.client.config.client-api.url=http://localhost:" + CLIENT_API_MOCK.port(),
                "spring.cloud.openfeign.client.config.sales-api.url=http://localhost:" + SALES_API_MOCK.port(),
                "spring.cloud.openfeign.client.config.keycloak.url=http://localhost:" + KEYCLOAK_MOCK.port(),
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:"
                        + KEYCLOAK_MOCK.port()
                        + "/realms/dealership/protocol/openid-connect/certs",
                "keycloak.base-url=http://localhost:" + KEYCLOAK_MOCK.port()
        ).applyTo(context.getEnvironment());
    }
}
