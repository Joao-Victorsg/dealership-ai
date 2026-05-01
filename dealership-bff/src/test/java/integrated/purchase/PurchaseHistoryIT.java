package integrated.purchase;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

class PurchaseHistoryIT extends BaseIT {

    private static final String HISTORY_RESPONSE = """
            {
              "content": [
                {
                  "id": "9b2e7f4c-0000-0000-0000-000000000001",
                  "registeredAt": "2026-04-26T14:00:00Z",
                  "status": "COMPLETED",
                  "vehicle": {
                    "id": "3f8a1c2d-0000-0000-0000-000000000001",
                    "model": "Civic",
                    "manufacturer": "Honda",
                    "manufacturingYear": 2023,
                    "externalColor": "White",
                    "vin": "1HGBH41JXMN109186",
                    "category": "SEDAN",
                    "listedValue": 145000.00
                  },
                  "client": {"firstName":"João","lastName":"Silva","cpf":"52998224725"}
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "number": 0,
              "size": 20
            }
            """;

    private static final String EMPTY_HISTORY = """
            {"content":[],"totalElements":0,"totalPages":0,"number":0,"size":20}
            """;

    private String clientToken;

    @BeforeEach
    void setUp() {
        EnvironmentInitializer.getSalesApiMock().resetAll();
        EnvironmentInitializer.getSalesApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/api/v1/sales/me"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(HISTORY_RESPONSE)));

        clientToken = JwtTestUtils.generateToken("sub-123", List.of("CLIENT"), "joao@example.com");
    }

    @Test
    void shouldReturnPaginatedHistoryForAuthenticatedClient() {
        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].status", equalTo("COMPLETED"))
                .body("meta.totalElements", equalTo(1))
                .body("meta.requestId", notNullValue());

        // Verify Sales API was actually called (no caching)
        EnvironmentInitializer.getSalesApiMock().verify(1,
                WireMock.getRequestedFor(urlPathEqualTo("/api/v1/sales/me")));
    }

    @Test
    void shouldReturnEmptyArrayWhenNoHistory() {
        EnvironmentInitializer.getSalesApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/api/v1/sales/me"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(EMPTY_HISTORY)));

        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(200)
                .body("data", hasSize(0));
    }

    @Test
    void salesApiAlwaysCalledVerifyNoCaching() {
        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(200);

        RestAssured.given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(200);

        // Both calls must reach the Sales API (no Redis caching)
        EnvironmentInitializer.getSalesApiMock().verify(2,
                WireMock.getRequestedFor(urlPathEqualTo("/api/v1/sales/me")));
    }

    @Test
    void shouldReturn401ForUnauthenticatedRequest() {
        RestAssured.given()
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(401);
    }
}
