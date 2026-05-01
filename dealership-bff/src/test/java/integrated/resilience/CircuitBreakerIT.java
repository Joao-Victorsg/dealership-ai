package integrated.resilience;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;

class CircuitBreakerIT extends BaseIT {

    @BeforeEach
    void resetMocks() {
        EnvironmentInitializer.getCarApiMock().resetAll();
    }

    @Test
    void shouldReturnDownstreamUnavailableWhenCarApiReturns500() {
        EnvironmentInitializer.getCarApiMock().stubFor(
                WireMock.get(WireMock.urlPathMatching("/api/v1/cars.*"))
                        .willReturn(aResponse().withStatus(500)));

        RestAssured.given()
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(503)
                .body("error.code", equalTo("DOWNSTREAM_UNAVAILABLE"));
    }
}
