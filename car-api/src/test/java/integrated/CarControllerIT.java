package integrated;

import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

class CarControllerIT extends BaseIT {

    private static final String URL = "/api/v1/cars";
    private static final String URL_WITH_ID = "/api/v1/cars/{id}";

    @DisplayName("Given a valid request to create a car, then return 201")
    @Test
    void givenValidRequestToCreateCarThenReturn201() {
        var body = createCarJson("ABCDE1234567890IT");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .body(body)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("data.id", notNullValue())
                .body("data.vin", equalTo("ABCDE1234567890IT"))
                .body("data.status", equalTo("AVAILABLE"))
                .body("data.model", equalTo("Tesla Model 3"))
                .header("Location", notNullValue());
    }

    @DisplayName("Given an unauthenticated request to create a car, then return 401")
    @Test
    void givenUnauthenticatedRequestToCreateCarThenReturn401() {
        var body = createCarJson("UNAUT1234567890AB");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Given a duplicate VIN, then return 409")
    @Test
    void givenDuplicateVinThenReturn409() {
        var body = createCarJson("DUPL01234567890AB");
        createCar(body);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .body(body)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @DisplayName("Given an invalid request body, then return 400")
    @Test
    void givenInvalidRequestBodyThenReturn400() {
        var invalidBody = """
                {
                    "model": "",
                    "vin": "INVALID"
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .body(invalidBody)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("Given a valid public request, then list cars returns 200")
    @Test
    void givenValidPublicRequestThenListCarsReturns200() {
        var body = createCarJson("LIST01234567890AB");
        createCar(body);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .when().get(URL)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content", notNullValue())
                .body("data.page.totalElements", greaterThanOrEqualTo(1));
    }

    @DisplayName("Given a valid car ID, then get car returns 200")
    @Test
    void givenValidCarIdThenGetCarReturns200() {
        var body = createCarJson("GETBY1234567890AB");
        var carId = createCarAndGetId(body);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .pathParam("id", carId)
                .when().get(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.id", equalTo(carId))
                .body("data.vin", equalTo("GETBY1234567890AB"));
    }

    @DisplayName("Given an unknown car ID, then return 404")
    @Test
    void givenUnknownCarIdThenReturn404() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .pathParam("id", "00000000-0000-0000-0000-000000000001")
                .when().get(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("data.message", notNullValue());
    }

    @DisplayName("Given a valid update request, then return 200")
    @Test
    void givenValidUpdateRequestThenReturn200() {
        var body = createCarJson("UPDAT1234567890AB");
        var carId = createCarAndGetId(body);

        var updateBody = """
                { "status": "UNAVAILABLE" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .pathParam("id", carId)
                .body(updateBody)
                .when().patch(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.status", equalTo("UNAVAILABLE"));
    }

    @DisplayName("Given a sold car, then update returns 422")
    @Test
    void givenSoldCarThenUpdateReturns422() {
        var body = createCarJson("SOLD01234567890AB");
        var carId = createCarAndGetId(body);

        // First mark as SOLD
        var soldBody = """
                { "status": "SOLD" }
                """;
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .pathParam("id", carId)
                .body(soldBody)
                .when().patch(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.OK.value());

        // Then try to update again
        var updateBody = """
                { "status": "AVAILABLE" }
                """;
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .pathParam("id", carId)
                .body(updateBody)
                .when().patch(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @DisplayName("Given an unauthenticated request to update a car, then return 401")
    @Test
    void givenUnauthenticatedRequestToUpdateCarThenReturn401() {
        var body = createCarJson("NOAUT1234567890AB");
        var carId = createCarAndGetId(body);

        var updateBody = """
                { "status": "UNAVAILABLE" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .pathParam("id", carId)
                .body(updateBody)
                .when().patch(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Given a wrong role to update a car, then return 403")
    @Test
    void givenWrongRoleToUpdateCarThenReturn403() {
        var body = createCarJson("FORBID234567890AB");
        var carId = createCarAndGetId(body);

        var updateBody = """
                { "status": "UNAVAILABLE" }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("VIEWER"))
                .pathParam("id", carId)
                .body(updateBody)
                .when().patch(URL_WITH_ID)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @DisplayName("Given ADMIN role, then car creation returns 201")
    @Test
    void givenAdminRoleThenCarCreationReturns201() {
        var body = createCarJson("ADMIN1234567890AB");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("ADMIN"))
                .body(body)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    private void createCar(String body) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + JwtTestUtils.generateToken("STAFF"))
                .body(body)
                .when().post(URL)
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    private String createCarAndGetId(String body) {
        return RestAssured.given()
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

    private String createCarJson(String vin) {
        return """
                {
                    "model": "Tesla Model 3",
                    "manufacturingYear": 2022,
                    "manufacturer": "Tesla",
                    "externalColor": "White",
                    "internalColor": "Black",
                    "vin": "%s",
                    "status": "AVAILABLE",
                    "category": "SEDAN",
                    "kilometers": 10000,
                    "isNew": false,
                    "propulsionType": "ELECTRIC",
                    "listedValue": 50000.00
                }
                """.formatted(vin);
    }
}
