package integrated.profile;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class ProfileIT extends BaseIT {

    private static final String CLIENT_RESPONSE = """
            {
              "id":"7c3e9f1a-0000-0000-0000-000000000001",
              "keycloakId":"sub-123",
              "firstName":"João",
              "lastName":"Silva",
              "cpf":"52998224725",
              "phone":"11987654321",
              "createdAt":"2026-01-10T09:30:00Z",
              "address": {
                "street":"Avenida Paulista",
                "number":"1000",
                "complement":"Apto 42",
                "neighborhood":"Bela Vista",
                "city":"São Paulo",
                "state":"SP",
                "cep":"01310100"
              }
            }
            """;

    private String clientToken;

    @BeforeEach
    void stubClientApi() {
        EnvironmentInitializer.getClientApiMock().resetAll();
        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/clients/me"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(CLIENT_RESPONSE)));

        clientToken = JwtTestUtils.generateToken("sub-123", List.of("CLIENT"), "joao@example.com");
    }

    @Test
    void shouldReturn200WithProfileDataForAuthenticatedClient() {
        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(200)
                .body("data.firstName", equalTo("João"))
                .body("data.email", equalTo("joao@example.com"))
                .body("data.address.city", equalTo("São Paulo"))
                .body("meta.requestId", notNullValue());
    }

    @Test
    void shouldReturn401ForUnauthenticatedRequest() {
        RestAssured.given()
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn503WhenClientApiUnavailable() {
        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/clients/me"))
                        .willReturn(aResponse().withStatus(503)));

        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(503)
                .body("error.code", equalTo("DOWNSTREAM_UNAVAILABLE"));
    }
}
