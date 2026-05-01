package integrated.security;

import br.com.dealership.dealershibff.config.OAuth2LoginSuccessHandler;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

class InputSanitizationIT extends BaseIT {

    @SuppressWarnings("rawtypes")
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void resetMocks() {
        EnvironmentInitializer.getKeycloakMock().resetAll();
        EnvironmentInitializer.getClientApiMock().resetAll();
        EnvironmentInitializer.registerDefaultKeycloakStubs();
    }

    @Test
    void shouldReturn400ForInvalidCpfOnRegister() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .cookie("SESSION", createAuthenticatedSession())
                .body("""
                        {"cpf":"00000000000","phone":"+55 (11) 99988-7766","cep":"01310100","streetNumber":"100"}
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400ForMaliciousQueryParam() {
        RestAssured.given()
                .param("q", "<script>alert('xss')</script>")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @SuppressWarnings("unchecked")
    private String createAuthenticatedSession() {
        final String accessToken = JwtTestUtils.generateToken(
                "sanitize-test-user", List.of("CLIENT"), "sanitize@test.com", "Test", "User");
        final Session session = (Session) sessionRepository.createSession();
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN, accessToken);
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY, Instant.now().plusSeconds(3600));
        sessionRepository.save(session);
        return Base64.getEncoder().encodeToString(session.getId().getBytes(StandardCharsets.UTF_8));
    }
}
