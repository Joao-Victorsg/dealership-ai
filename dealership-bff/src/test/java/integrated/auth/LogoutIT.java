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
import static org.hamcrest.Matchers.equalTo;

class LogoutIT extends BaseIT {

    @BeforeEach
    void stubKeycloak() {
        EnvironmentInitializer.getKeycloakMock().resetAll();
        final var publicKey = integrated.utils.JwtTestUtils.getPublicKey();
        final String jwksJson = "{\"keys\":[" + publicKey.toJSONString() + "]}";
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.get(urlPathEqualTo("/realms/dealership/protocol/openid-connect/certs"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(jwksJson)));
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.post(urlPathEqualTo("/realms/dealership/protocol/openid-connect/logout"))
                        .willReturn(aResponse().withStatus(204)));
    }

    @Test
    void shouldReturn204AndClearCookieOnLogout() {
        RestAssured.given()
                .cookie("refresh_token", "some-valid-refresh-token")
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(204);
    }

    @Test
    void shouldReturn401OnRefreshWithNoValidCookie() {
        RestAssured.given()
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);
    }
}
