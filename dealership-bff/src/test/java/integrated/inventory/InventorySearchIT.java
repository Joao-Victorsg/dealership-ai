package integrated.inventory;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class InventorySearchIT extends BaseIT {

    private static final String SINGLE_CAR_RESPONSE = """
            {
              "content": [
                {
                  "id": "3f8a1c2d-0000-0000-0000-000000000002",
                  "model": "Corolla",
                  "manufacturer": "Toyota",
                  "manufacturingYear": 2022,
                  "externalColor": "Blue",
                  "internalColor": "Gray",
                  "vin": "JTDKB20U480101234",
                  "status": "AVAILABLE",
                  "category": "SEDAN",
                  "type": "HYBRID",
                  "isNew": false,
                  "kilometers": 25000.00,
                  "propulsionType": "FRONT_WHEEL_DRIVE",
                  "listedValue": 120000.00,
                  "imageKey": "cars/abc456.jpg",
                  "optionalItems": [],
                  "registrationDate": "2026-01-10T09:00:00Z"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "number": 0,
              "size": 20
            }
            """;

    @BeforeEach
    void stubCarApi() {
        EnvironmentInitializer.getCarApiMock().resetAll();
        EnvironmentInitializer.getCarApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/api/v1/cars"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(SINGLE_CAR_RESPONSE)));
    }

    @Test
    void shouldForwardFreeTextQueryToCarApi() {
        RestAssured.given()
                .queryParam("q", "corolla")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(200)
                .body("data[0].model", equalTo("Corolla"));

        EnvironmentInitializer.getCarApiMock().verify(
                WireMock.getRequestedFor(urlPathEqualTo("/api/v1/cars"))
                        .withQueryParam("q", WireMock.equalTo("corolla")));
    }

    @Test
    void shouldReturn400ForInvalidPriceRange() {
        RestAssured.given()
                .queryParam("priceMin", "200000")
                .queryParam("priceMax", "100000")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400ForInvalidYearRange() {
        RestAssured.given()
                .queryParam("yearMin", "2025")
                .queryParam("yearMax", "2020")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldSanitizeMaliciousQueryParam() {
        RestAssured.given()
                .queryParam("q", "<script>alert(1)</script>corolla")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(200);

        EnvironmentInitializer.getCarApiMock().verify(
                WireMock.getRequestedFor(urlPathEqualTo("/api/v1/cars"))
                        .withQueryParam("q", WireMock.matching("^[^<>]*$")));
    }
}
