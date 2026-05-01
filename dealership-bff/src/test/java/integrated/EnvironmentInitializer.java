package integrated;

import com.github.tomakehurst.wiremock.WireMockServer;
import integrated.utils.JwtTestUtils;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

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

        // Register JWKS + OIDC discovery stubs so OAuth2 client auto-configures at startup
        registerDefaultKeycloakStubs();
    }

    public static String getKeycloakBaseUrl() {
        return "http://localhost:" + KEYCLOAK_MOCK.port();
    }

    /** Registers JWKS and OIDC discovery stubs on the Keycloak mock. Call this after resetAll(). */
    public static void registerDefaultKeycloakStubs() {
        final var publicKey = JwtTestUtils.getPublicKey();
        final String jwksJson;
        try {
            jwksJson = "{\"keys\":[" + publicKey.toJSONString() + "]}";
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test JWKS", e);
        }

        final String baseUrl = getKeycloakBaseUrl();
        final String issuer = baseUrl + "/realms/dealership";

        KEYCLOAK_MOCK.stubFor(
                get(urlPathEqualTo("/realms/dealership/protocol/openid-connect/certs"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(jwksJson)));

        KEYCLOAK_MOCK.stubFor(
                get(urlPathEqualTo("/realms/dealership/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(buildOidcDiscoveryJson(baseUrl, issuer))));
    }

    private static String buildOidcDiscoveryJson(final String baseUrl, final String issuer) {
        return """
                {
                  "issuer": "%s",
                  "authorization_endpoint": "%s/realms/dealership/protocol/openid-connect/auth",
                  "token_endpoint": "%s/realms/dealership/protocol/openid-connect/token",
                  "userinfo_endpoint": "%s/realms/dealership/protocol/openid-connect/userinfo",
                  "jwks_uri": "%s/realms/dealership/protocol/openid-connect/certs",
                  "end_session_endpoint": "%s/realms/dealership/protocol/openid-connect/logout",
                  "response_types_supported": ["code"],
                  "subject_types_supported": ["public"],
                  "id_token_signing_alg_values_supported": ["RS256"],
                  "scopes_supported": ["openid", "profile", "email", "offline_access"],
                  "code_challenge_methods_supported": ["S256"],
                  "grant_types_supported": ["authorization_code", "refresh_token"]
                }
                """.formatted(issuer, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);
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
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:"
                        + KEYCLOAK_MOCK.port()
                        + "/realms/dealership/protocol/openid-connect/certs",
                "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:"
                        + KEYCLOAK_MOCK.port()
                        + "/realms/dealership"
        ).applyTo(context.getEnvironment());
    }
}
