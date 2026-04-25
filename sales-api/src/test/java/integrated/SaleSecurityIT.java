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

class SaleSecurityIT extends BaseIT {

    SaleSecurityIT(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Test
    void noJwtReturns401OnPostSales() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(401);
    }

    @Test
    void noJwtReturns401OnGetSales() {
        given()
        .when()
                .get("/api/v1/sales")
        .then()
                .statusCode(401);
    }

    @Test
    void noJwtReturns401OnGetSaleById() {
        given()
        .when()
                .get("/api/v1/sales/{id}", UUID.randomUUID())
        .then()
                .statusCode(401);
    }

    @Test
    void noJwtReturns401OnGetStaffSales() {
        given()
        .when()
                .get("/api/v1/sales/staff")
        .then()
                .statusCode(401);
    }

    @Test
    void wrongAudienceReturns401() {
        String wrongAudToken = JwtTestUtils.generateTokenWithAudience(UUID.randomUUID(), "wrong-audience", "CLIENT");

        given()
                .header("Authorization", "Bearer " + wrongAudToken)
        .when()
                .get("/api/v1/sales")
        .then()
                .statusCode(401);
    }

    @Test
    void clientRoleCannotAccessStaffEndpoint() {
        String token = JwtTestUtils.generateToken(UUID.randomUUID(), "CLIENT");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/v1/sales/staff")
        .then()
                .statusCode(403);
    }

    @Test
    void clientGets403NotFoundWhenAccessingAnotherClientSale() {
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
    void staffCanAccessAnySaleById() {
        UUID clientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        String clientToken = JwtTestUtils.generateToken(clientId, "CLIENT");
        String staffToken = JwtTestUtils.generateToken(staffId, "STAFF");

        String saleId = given()
                .header("Authorization", "Bearer " + clientToken)
                .contentType(ContentType.JSON)
                .body(buildValidRequest(clientId))
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("data.id");

        given()
                .header("Authorization", "Bearer " + staffToken)
        .when()
                .get("/api/v1/sales/{id}", saleId)
        .then()
                .statusCode(200);
    }

    @Test
    void adminCanAccessStaffEndpoint() {
        String adminToken = JwtTestUtils.generateToken(UUID.randomUUID(), "ADMIN");

        given()
                .header("Authorization", "Bearer " + adminToken)
        .when()
                .get("/api/v1/sales/staff")
        .then()
                .statusCode(200);
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
}
