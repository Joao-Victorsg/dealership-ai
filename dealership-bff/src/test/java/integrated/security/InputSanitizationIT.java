package integrated.security;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class InputSanitizationIT extends BaseIT {

    @Test
    void shouldReturn400ForInvalidCpfOnRegister() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"user@test.com","password":"password123","firstName":"A","lastName":"B","cpf":"00000000000","phone":"11999887766","cep":"01310100"}
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400ForMaliciousQueryParam() {
        RestAssured.given()
                .param("q", "<script>alert('xss')</script>")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldSetHttpOnlyFlagOnRefreshTokenCookie() {
        EnvironmentInitializer.getKeycloakMock().resetAll();

        final var publicKey = JwtTestUtils.getPublicKey();
        final String jwksJson = "{\"keys\":[" + publicKey.toJSONString() + "]}";
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.get(urlPathEqualTo("/realms/dealership/protocol/openid-connect/certs"))
                        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(jwksJson)));
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.post(urlPathEqualTo("/realms/dealership/protocol/openid-connect/token"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {"access_token":"test-token","refresh_token":"refresh-token","expires_in":3600,"refresh_expires_in":86400,"token_type":"Bearer"}
                                        """)));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"user@test.com\",\"password\":\"password123\"}")
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .header("Set-Cookie", containsString("HttpOnly"));
    }
}
