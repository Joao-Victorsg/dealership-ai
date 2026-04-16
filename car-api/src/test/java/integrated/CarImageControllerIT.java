package integrated;

import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

class CarImageControllerIT extends BaseIT {

    private static final String URL = "/api/v1/cars";
    private static final String PRESIGNED_URL_PATH = "/api/v1/cars/{id}/image/presigned-url";

    private String testCarId;

    @BeforeEach
    void setUpCar() {
        var body = """
                {
                    "model": "Tesla Model 3",
                    "manufacturingYear": 2022,
                    "manufacturer": "Tesla",
                    "externalColor": "White",
                    "internalColor": "Black",
                    "vin": "IMG%s",
                    "status": "AVAILABLE",
                    "category": "SEDAN",
                    "kilometers": 10000,
                    "isNew": false,
                    "propulsionType": "ELECTRIC",
                    "listedValue": 50000.00
                }
                """.formatted(String.valueOf(System.nanoTime()).substring(0, 14));

        testCarId = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .body(body)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .jsonPath()
                .getString("data.id");
    }

    @DisplayName("Given a valid presigned URL request, then return 200")
    @Test
    void givenValidPresignedUrlRequestThenReturn200() {
        var requestBody = """
                { "contentType": "image/jpeg" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .pathParam("id", testCarId)
                .body(requestBody)
                .when().post(PRESIGNED_URL_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.presignedUrl", notNullValue())
                .body("data.objectKey", notNullValue())
                .body("data.expiresIn", equalTo(900));
    }

    @DisplayName("Given an unauthenticated request, then return 401")
    @Test
    void givenUnauthenticatedRequestThenReturn401() {
        var requestBody = """
                { "contentType": "image/jpeg" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .pathParam("id", testCarId)
                .body(requestBody)
                .when().post(PRESIGNED_URL_PATH)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Given an unknown car ID, then return 404")
    @Test
    void givenUnknownCarIdThenReturn404() {
        var requestBody = """
                { "contentType": "image/png" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .pathParam("id", "00000000-0000-0000-0000-000000000001")
                .body(requestBody)
                .when().post(PRESIGNED_URL_PATH)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @DisplayName("Given an unsupported content type, then return 400")
    @Test
    void givenUnsupportedContentTypeThenReturn400() {
        var requestBody = """
                { "contentType": "application/pdf" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .pathParam("id", testCarId)
                .body(requestBody)
                .when().post(PRESIGNED_URL_PATH)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("Given a non-staff role, then return 403")
    @Test
    void givenNonStaffRoleThenReturn403() {
        var requestBody = """
                { "contentType": "image/jpeg" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("VIEWER"))
                .pathParam("id", testCarId)
                .body(requestBody)
                .when().post(PRESIGNED_URL_PATH)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }
}
