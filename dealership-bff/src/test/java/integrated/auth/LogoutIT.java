package integrated.auth;

import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class LogoutIT extends BaseIT {

    @BeforeEach
    void resetMocks() {
        EnvironmentInitializer.getKeycloakMock().resetAll();
        EnvironmentInitializer.registerDefaultKeycloakStubs();
    }

    @Test
    void shouldRedirectOnLogout() {
        given()
                .redirects().follow(false)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(302);
    }
}
