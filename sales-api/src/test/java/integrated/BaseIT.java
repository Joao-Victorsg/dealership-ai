package integrated;

import br.com.dealership.salesapi.SalesApiApplication;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = SalesApiApplication.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("it")
@ContextConfiguration(initializers = EnvironmentInitializer.class)
public abstract class BaseIT {

    @LocalServerPort
    private int port;

    protected final JdbcTemplate jdbcTemplate;

    protected BaseIT(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void configureRestAssuredAndResetState() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        jdbcTemplate.execute("TRUNCATE TABLE sales");
    }
}
