package integrated.purchase;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class PurchaseIT extends BaseIT {

    private static final UUID CAR_ID = UUID.fromString("3f8a1c2d-0000-0000-0000-000000000001");
    private String clientToken;

    private static final String AVAILABLE_CAR = """
            {
              "id": "%s",
              "model": "Civic",
              "manufacturer": "Honda",
              "manufacturingYear": 2023,
              "externalColor": "White",
              "internalColor": "Black",
              "vin": "1HGBH41JXMN109186",
              "status": "AVAILABLE",
              "category": "SEDAN",
              "type": "CAR",
              "isNew": true,
              "kilometers": 0,
              "propulsionType": "GASOLINE",
              "listedValue": 145000.00
            }
            """.formatted(CAR_ID);

    private static final String CLIENT_RESPONSE = """
            {
              "id": "7c3e9f1a-0000-0000-0000-000000000001",
              "keycloakId": "sub-123",
              "firstName": "João",
              "lastName": "Silva",
              "cpf": "52998224725",
              "phone": "11987654321",
              "createdAt": "2026-01-10T09:30:00Z"
            }
            """;

    private static final String SALE_RESPONSE = """
            {
              "id": "9b2e7f4c-0000-0000-0000-000000000001",
              "registeredAt": "2026-04-26T14:00:00Z",
              "status": "COMPLETED",
              "vehicle": {
                "id": "%s",
                "model": "Civic",
                "manufacturer": "Honda",
                "manufacturingYear": 2023,
                "externalColor": "White",
                "vin": "1HGBH41JXMN109186",
                "category": "SEDAN",
                "listedValue": 145000.00
              },
              "client": {
                "firstName": "João",
                "lastName": "Silva",
                "cpf": "52998224725"
              }
            }
            """.formatted(CAR_ID);

    @BeforeEach
    void setUp() {
        EnvironmentInitializer.getCarApiMock().resetAll();
        EnvironmentInitializer.getClientApiMock().resetAll();
        EnvironmentInitializer.getSalesApiMock().resetAll();

        EnvironmentInitializer.getCarApiMock().stubFor(
                WireMock.get(WireMock.urlPathMatching("/api/v1/cars/.*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(AVAILABLE_CAR)));

        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/clients/me"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(CLIENT_RESPONSE)));

        EnvironmentInitializer.getSalesApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/api/v1/sales"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(201)
                                .withBody(SALE_RESPONSE)));

        clientToken = JwtTestUtils.generateToken("sub-123", List.of("CLIENT"), "joao@example.com");
    }

    @Test
    void shouldReturn201AndPurchaseResponseOnSuccessfulPurchase() {
        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .contentType(ContentType.JSON)
                .body("{\"carId\":\"" + CAR_ID + "\"}")
                .when()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.status", equalTo("COMPLETED"))
                .body("meta.requestId", notNullValue());

        // Verify Sales API was called exactly once (no retry)
        EnvironmentInitializer.getSalesApiMock().verify(1,
                WireMock.postRequestedFor(urlPathEqualTo("/api/v1/sales")));
    }

    @Test
    void shouldReturn409WhenSalesApiReturns409() {
        EnvironmentInitializer.getSalesApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/api/v1/sales"))
                        .willReturn(aResponse().withStatus(409)));

        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .contentType(ContentType.JSON)
                .body("{\"carId\":\"" + CAR_ID + "\"}")
                .when()
                .post("/api/v1/purchases")
                .then()
                .statusCode(409)
                .body("error.code", equalTo("CAR_NOT_AVAILABLE"));

        // Verify exactly one call (no retry)
        EnvironmentInitializer.getSalesApiMock().verify(1,
                WireMock.postRequestedFor(urlPathEqualTo("/api/v1/sales")));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"carId\":\"" + CAR_ID + "\"}")
                .when()
                .post("/api/v1/purchases")
                .then()
                .statusCode(401);
    }
}
