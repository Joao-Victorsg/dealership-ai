package integrated.security;

import integrated.BaseIT;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

class ForbiddenRoleIT extends BaseIT {

    @Test
    void shouldReturn403ForProfileWithAdminRole() {
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
