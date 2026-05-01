package integrated.auth;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class RegisterIT extends BaseIT {

    private static final String ADMIN_TOKEN_RESPONSE = """
            {"access_token":"admin-token","token_type":"Bearer","expires_in":300}
            """;

    private static final String CLIENT_RESPONSE = """
            {
              "id":"3f8a1c2d-0000-0000-0000-000000000010",
              "keycloakId":"user@test.com",
              "firstName":"John",
              "lastName":"Doe",
              "cpf":"52998224725",
              "phone":"11999887766",
              "createdAt":"2026-04-26T10:00:00Z"
            }
            """;

    @BeforeEach
    void stubApis() {
        EnvironmentInitializer.getKeycloakMock().resetAll();
        EnvironmentInitializer.getClientApiMock().resetAll();

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
                                .withBody(ADMIN_TOKEN_RESPONSE)));

        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.post(urlPathEqualTo("/admin/realms/dealership/users"))
                        .willReturn(aResponse().withStatus(201)));

        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/clients"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(201)
                                .withBody(CLIENT_RESPONSE)));
    }

    @Test
    void shouldReturn201OnSuccessfulRegistration() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email":"user@test.com",
                          "password":"password123",
                          "firstName":"John",
                          "lastName":"Doe",
                          "cpf":"52998224725",
                          "phone":"11999887766",
                          "cep":"01310100"
                        }
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("meta.requestId", notNullValue());
    }

    @Test
    void shouldReturn400ForInvalidCpfFormat() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email":"user@test.com",
                          "password":"password123",
                          "firstName":"John",
                          "lastName":"Doe",
                          "cpf":"00000000000",
                          "phone":"11999887766",
                          "cep":"01310100"
                        }
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldCallKeycloakDeleteWhenClientApiFailsAndReturn503() {
        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/clients"))
                        .willReturn(aResponse().withStatus(500)));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email":"user@test.com",
                          "password":"password123",
                          "firstName":"John",
                          "lastName":"Doe",
                          "cpf":"52998224725",
                          "phone":"11999887766",
                          "cep":"01310100"
                        }
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(503)
                .body("error.code", equalTo("DOWNSTREAM_UNAVAILABLE"));

        EnvironmentInitializer.getKeycloakMock().verify(
                WireMock.deleteRequestedFor(
                        WireMock.urlMatching("/admin/realms/dealership/users/.*")));
    }

    @Test
    void shouldReturn422WhenKeycloakReturns409() {
        EnvironmentInitializer.getKeycloakMock().stubFor(
                WireMock.post(urlPathEqualTo("/admin/realms/dealership/users"))
                        .willReturn(aResponse().withStatus(409)));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email":"existing@test.com",
                          "password":"password123",
                          "firstName":"John",
                          "lastName":"Doe",
                          "cpf":"52998224725",
                          "phone":"11999887766",
                          "cep":"01310100"
                        }
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(422)
                .body("error.code", equalTo("DUPLICATE_IDENTITY"));
    }
}
