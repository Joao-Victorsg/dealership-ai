package integrated;

import br.com.dealership.salesapi.domain.entity.CarStatus;
import br.com.dealership.salesapi.dto.request.AddressSnapshotRequest;
import br.com.dealership.salesapi.dto.request.CarSnapshotRequest;
import br.com.dealership.salesapi.dto.request.ClientSnapshotRequest;
import br.com.dealership.salesapi.dto.request.RegisterSaleRequest;
import integrated.utils.JwtTestUtils;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class SaleRetrievalIT extends BaseIT {

    SaleRetrievalIT(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Test
    void clientCanListOwnSalesWithCorrectCount() {
        UUID clientA = UUID.randomUUID();
        String tokenA = JwtTestUtils.generateToken(clientA, "CLIENT");

        registerSale(tokenA, clientA);
        registerSale(tokenA, clientA);
        registerSale(tokenA, clientA);

        given()
                .header("Authorization", "Bearer " + tokenA)
        .when()
                .get("/api/v1/sales")
        .then()
                .statusCode(200)
                .body("data.content", hasSize(3));
    }

    @Test
    void clientGetsEmptyPageForNewClient() {
        UUID newClient = UUID.randomUUID();
        String token = JwtTestUtils.generateToken(newClient, "CLIENT");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/v1/sales")
        .then()
                .statusCode(200)
                .body("data.content", hasSize(0));
    }

    @Test
    void defaultPageSizeIs20() {
        UUID clientId = UUID.randomUUID();
        String token = JwtTestUtils.generateToken(clientId, "CLIENT");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/v1/sales")
        .then()
                .statusCode(200)
                .body("data.size", equalTo(20));
    }

    @Test
    void clientCanGetOwnSaleById() {
        UUID clientId = UUID.randomUUID();
        String token = JwtTestUtils.generateToken(clientId, "CLIENT");

        String saleId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(buildValidRequest(clientId))
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/v1/sales/{id}", saleId)
        .then()
                .statusCode(200)
                .body("data.id", equalTo(saleId));
    }

    @Test
    void clientBCannotAccessClientASaleIdReturns403() {
        UUID clientA = UUID.randomUUID();
        UUID clientB = UUID.randomUUID();
        String tokenA = JwtTestUtils.generateToken(clientA, "CLIENT");
        String tokenB = JwtTestUtils.generateToken(clientB, "CLIENT");

        String saleId = given()
                .header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(buildValidRequest(clientA))
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .get("/api/v1/sales/{id}", saleId)
        .then()
                .statusCode(403);
    }

    @Test
    void cachedSaleStillEnforcesOwnershipCheck() {
        UUID clientA = UUID.randomUUID();
        UUID clientB = UUID.randomUUID();
        String tokenA = JwtTestUtils.generateToken(clientA, "CLIENT");
        String tokenB = JwtTestUtils.generateToken(clientB, "CLIENT");

        String saleId = given()
                .header("Authorization", "Bearer " + tokenA)
                .contentType(ContentType.JSON)
                .body(buildValidRequest(clientA))
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + tokenA)
        .when()
                .get("/api/v1/sales/{id}", saleId)
        .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + tokenB)
        .when()
                .get("/api/v1/sales/{id}", saleId)
        .then()
                .statusCode(403);
    }

    private RegisterSaleRequest buildValidRequest(UUID clientId) {
        var addr = new AddressSnapshotRequest("Main St", "100", null, "Downtown",
                "São Paulo", "SP", "01001-000");
        var client = new ClientSnapshotRequest("John", "Doe", "12345678901",
                "john@example.com", addr);
        var car = new CarSnapshotRequest("Model", "Brand", "Red", "Black", 2020,
                List.of(), "Sedan", "Luxury", "ABC12345678901234",
                BigDecimal.valueOf(10000), CarStatus.AVAILABLE);
        return new RegisterSaleRequest(UUID.randomUUID(), clientId, client, car);
    }

    private void registerSale(String token, UUID clientId) {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(buildValidRequest(clientId))
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(201);
    }
}
