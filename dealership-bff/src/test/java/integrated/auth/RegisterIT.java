package integrated.auth;

import br.com.dealership.dealershibff.config.OAuth2LoginSuccessHandler;
import com.github.tomakehurst.wiremock.client.WireMock;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class RegisterIT extends BaseIT {

    @SuppressWarnings("rawtypes")
    @Autowired
    private SessionRepository sessionRepository;

    private static final String CLIENT_RESPONSE = """
            {
              "id":"3f8a1c2d-0000-0000-0000-000000000010",
              "keycloakId":"kc-reg-user",
              "firstName":"John",
              "lastName":"Doe",
              "cpf":"52998224725",
              "phone":"11999887766",
              "createdAt":"2026-04-26T10:00:00Z"
            }
            """;

    private static final String REGISTER_BODY = """
            {
              "cpf":"52998224725",
              "phone":"+55 (11) 99988-7766",
              "cep":"01310100",
              "streetNumber":"100"
            }
            """;

    @BeforeEach
    void stubApis() {
        EnvironmentInitializer.getClientApiMock().resetAll();
        EnvironmentInitializer.getKeycloakMock().resetAll();
        EnvironmentInitializer.registerDefaultKeycloakStubs();

        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/clients"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(201)
                                .withBody(CLIENT_RESPONSE)));
    }

    @Test
    void shouldReturn201OnSuccessfulRegistration() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .cookie("SESSION", createAuthenticatedSession())
                .body(REGISTER_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("meta.requestId", notNullValue())
                .body("data.firstName", equalTo("John"));
    }

    @Test
    void shouldReturn400ForInvalidCpfFormat() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .cookie("SESSION", createAuthenticatedSession())
                .body("""
                        {
                          "cpf":"00000000000",
                          "phone":"+55 (11) 99988-7766",
                          "cep":"01310100",
                          "streetNumber":"100"
                        }
                        """)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn503WhenClientApiFailsAndReturn503() {
        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/clients"))
                        .willReturn(aResponse().withStatus(500)));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .cookie("SESSION", createAuthenticatedSession())
                .body(REGISTER_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(503)
                .body("error.code", equalTo("DOWNSTREAM_UNAVAILABLE"));
    }

    @Test
    void shouldReturn422WhenClientApiReturns409() {
        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.post(urlPathEqualTo("/clients"))
                        .willReturn(aResponse().withStatus(409)));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .cookie("SESSION", createAuthenticatedSession())
                .body(REGISTER_BODY)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(422)
                .body("error.code", equalTo("DUPLICATE_IDENTITY"));
    }

    @SuppressWarnings("unchecked")
    private String createAuthenticatedSession() {
        final String accessToken = JwtTestUtils.generateToken(
                "kc-reg-user", List.of("CLIENT"), "kc-reg-user@test.com", "John", "Doe");
        final Session session = (Session) sessionRepository.createSession();
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN, accessToken);
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY, Instant.now().plusSeconds(3600));
        sessionRepository.save(session);
        return Base64.getEncoder().encodeToString(session.getId().getBytes(StandardCharsets.UTF_8));
    }
}
