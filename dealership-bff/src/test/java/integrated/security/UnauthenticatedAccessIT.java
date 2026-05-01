package integrated.security;

import integrated.BaseIT;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;

class UnauthenticatedAccessIT extends BaseIT {

    @Test
    void shouldReturn401ForProfileWithoutToken() {
        RestAssured.given()
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn401ForGetPurchasesWithoutToken() {
        RestAssured.given()
                .when()
                .get("/api/v1/purchases")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn401ForPostPurchasesWithoutToken() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"carId\":\"3f8a1c2d-0000-0000-0000-000000000001\"}")
                .when()
                .post("/api/v1/purchases")
                .then()
                .statusCode(401);
    }
}
