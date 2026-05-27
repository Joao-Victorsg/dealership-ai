package integrated.security;

import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

class ForbiddenRoleIT extends BaseIT {

    @Test
    void shouldReturn403ForProfileWithAdminRole() {
        EnvironmentInitializer.getClientApiMock().stubFor(
                get(urlPathEqualTo("/clients/me"))
                        .willReturn(aResponse().withStatus(403)));

        final var adminToken = JwtTestUtils.generateToken("admin-sub", List.of("ADMIN"), "admin@test.com");

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldReturn403ForPurchaseHistoryWithAdminRole() {
        final var adminToken = JwtTestUtils.generateToken("admin-sub", List.of("ADMIN"), "admin@test.com");

        RestAssured.given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(403);
    }
}
