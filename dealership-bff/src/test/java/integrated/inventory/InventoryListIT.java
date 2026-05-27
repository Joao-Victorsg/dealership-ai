package integrated.inventory;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

class InventoryListIT extends BaseIT {

    private static final String CAR_LIST_RESPONSE = """
            {
              "data": {
                "content": [
                  {
                    "id": "3f8a1c2d-0000-0000-0000-000000000001",
                    "model": "Civic",
                    "manufacturer": "Honda",
                    "manufacturingYear": 2023,
                    "externalColor": "Pearl White",
                    "internalColor": "Black",
                    "vin": "1HGBH41JXMN109186",
                    "status": "AVAILABLE",
                    "category": "SEDAN",
                    "type": "GASOLINE",
                    "isNew": false,
                    "kilometers": 15000.00,
                    "propulsionType": "FRONT_WHEEL_DRIVE",
                    "listedValue": 145000.00,
                    "imageKey": "cars/abc123.jpg",
                    "optionalItems": ["Sunroof"],
                    "registrationDate": "2026-01-15T10:00:00Z"
                  }
                ],
                "page": {
                  "size": 20,
                  "number": 0,
                  "totalElements": 1,
                  "totalPages": 1
                }
              }
            }
            """;

    private static final String FILTER_OPTIONS_RESPONSE = """
            {
              "data": {
                "manufacturers": ["Honda", "Toyota"],
                "exteriorColors": ["Black", "White"]
              }
            }
            """;

    @BeforeEach
    void stubCarApi() {
        EnvironmentInitializer.getCarApiMock().resetAll();
        EnvironmentInitializer.getCarApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/api/v1/cars"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(CAR_LIST_RESPONSE)));
        EnvironmentInitializer.getCarApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/api/v1/cars/filter-options"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(FILTER_OPTIONS_RESPONSE)));
    }

    @Test
    void shouldReturnPaginatedEnvelopeForListRequest() {
        RestAssured.given()
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].model", equalTo("Civic"))
                .body("meta.page", equalTo(0))
                .body("meta.pageSize", equalTo(20))
                .body("meta.totalElements", equalTo(1))
                .body("meta.requestId", notNullValue());

        EnvironmentInitializer.getCarApiMock().verify(
                getRequestedFor(urlPathEqualTo("/api/v1/cars"))
                        .withQueryParam("status", WireMock.equalTo("AVAILABLE")));
    }

    @Test
    void shouldReturn503WhenCarApiReturns500() {
        EnvironmentInitializer.getCarApiMock().resetAll();
        EnvironmentInitializer.getCarApiMock().stubFor(
                WireMock.get(urlPathEqualTo("/api/v1/cars"))
                        .willReturn(aResponse().withStatus(500)));

        RestAssured.given()
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(503)
                .body("error.code", equalTo("DOWNSTREAM_UNAVAILABLE"));
    }

    @Test
    void shouldReturnFilterOptionsFromCarApi() {
        RestAssured.given()
                .when()
                .get("/api/v1/inventory/filter-options")
                .then()
                .statusCode(200)
                .body("data.manufacturers", hasItems("Honda", "Toyota"))
                .body("data.exteriorColors", hasItems("Black", "White"))
                .body("meta.requestId", notNullValue());

        EnvironmentInitializer.getCarApiMock().verify(
                getRequestedFor(urlPathEqualTo("/api/v1/cars/filter-options")));
    }
}
