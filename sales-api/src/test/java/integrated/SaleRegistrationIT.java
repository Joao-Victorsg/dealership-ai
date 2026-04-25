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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

class SaleRegistrationIT extends BaseIT {

    SaleRegistrationIT(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Test
    void registerSaleReturns201WithCorrectSaleValueAndSnsMessage() {
        UUID clientId = UUID.randomUUID();
        String token = JwtTestUtils.generateToken(clientId, "CLIENT");

        RegisterSaleRequest request = buildValidRequest(clientId, CarStatus.AVAILABLE,
                BigDecimal.valueOf(10000));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/sales")
        .then()
                .time(lessThan(3000L), MILLISECONDS)
                .statusCode(201)
                .header("Location", notNullValue())
                .body("data.saleValue", equalTo(11000.0f));
    }

    @Test
    void registerSaleReturns422WhenCarStatusIsSold() {
        UUID clientId = UUID.randomUUID();
        String token = JwtTestUtils.generateToken(clientId, "CLIENT");
        RegisterSaleRequest request = buildValidRequest(clientId, CarStatus.SOLD,
                BigDecimal.valueOf(10000));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(422);
    }

    @Test
    void registerSaleReturns403WhenClientIdMismatch() {
        UUID tokenClientId = UUID.randomUUID();
        UUID differentClientId = UUID.randomUUID();
        String token = JwtTestUtils.generateToken(tokenClientId, "CLIENT");
        RegisterSaleRequest request = buildValidRequest(differentClientId, CarStatus.AVAILABLE,
                BigDecimal.valueOf(10000));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/sales")
        .then()
                .statusCode(403);
    }

    private RegisterSaleRequest buildValidRequest(UUID clientId, CarStatus status,
                                                   BigDecimal listedValue) {
        return new RegisterSaleRequest(
                UUID.randomUUID(), clientId,
                buildClientSnapshot(),
                new CarSnapshotRequest("Model", "Brand", "Red", "Black", 2020,
                        List.of(), "Sedan", "Luxury", "ABC12345678901234",
                        listedValue, status)
        );
    }

    private ClientSnapshotRequest buildClientSnapshot() {
        var addr = new AddressSnapshotRequest("Main St", "100", null, "Downtown",
                "Sao Paulo", "SP", "01001-000");
        return new ClientSnapshotRequest("John", "Doe", "12345678901", "john@example.com", addr);
    }
}
