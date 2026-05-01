package integrated.auth;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class LoginIT extends BaseIT {

    private static final String TOKEN_RESPONSE = """
            {
              "access_token": "eyJhbGciOiJSUzI1NiJ9.test.signature",
              "refresh_token": "eyJhbGciOiJSUzI1NiJ9.refresh.signature",
              "expires_in": 3600,
              "refresh_expires_in": 86400,
              "token_type": "Bearer"
            }
            """;

    @BeforeEach
    void stubKeycloak() {
        EnvironmentInitializer.getKeycloakMock().resetAll();
        // Re-register JWKS stub after reset
        final var publicKey = integrated.utils.JwtTestUtils.getPublicKey();
        final String jwksJson = "{\"keys\":[" + publicKey.toJSONString() + "]}";
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.get(urlPathEqualTo("/realms/dealership/protocol/openid-connect/certs"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(jwksJson)));
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.post(urlPathEqualTo("/realms/dealership/protocol/openid-connect/token"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withBody(TOKEN_RESPONSE)));
    }

    @Test
    void shouldReturnAccessTokenAndSetHttpOnlyCookieOnLogin() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"user@test.com","password":"password123"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("data.accessToken", notNullValue())
                .header("Set-Cookie", containsString("refresh_token"))
                .header("Set-Cookie", containsString("HttpOnly"));
    }

    @Test
    void shouldReturn401EnvelopeForInvalidCredentials() {
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.post(urlPathEqualTo("/realms/dealership/protocol/openid-connect/token"))
                        .willReturn(aResponse().withStatus(401)));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"user@test.com","password":"wrong"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("error.code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldReturn400ForMissingEmail() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"","password":"password123"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }
}
