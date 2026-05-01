package integrated.auth;

import br.com.dealership.dealershibff.config.OAuth2LoginSuccessHandler;
import com.github.tomakehurst.wiremock.client.WireMock;
import integrated.BaseIT;
import integrated.EnvironmentInitializer;
import integrated.utils.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class PkceAuthFlowIT extends BaseIT {

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
    void shouldRedirectToPkceLoginWithCodeChallengeAndState() {
        final var response = given()
                .redirects().follow(false)
                .when()
                .get("/oauth2/authorization/keycloak")
                .then()
                .statusCode(302)
                .extract().response();

        final String location = response.header("Location");
        assertThat(location).contains("response_type=code");
        assertThat(location).contains("code_challenge_method=S256");
        assertThat(location).contains("state=");
        assertThat(location).contains("nonce=");
        assertThat(location).contains("code_challenge=");
        assertThat(location).contains("client_id=dealership-bff");
    }

    @Test
    void shouldReturn401ForUnauthenticatedCallToProtectedEndpoint() {
        given()
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldRedirectOnLogoutWithoutSession() {
        given()
                .redirects().follow(false)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(302);
    }

    @Test
    void shouldEstablishSessionCookieAfterPkceCallback() throws Exception {
        // Step 1: initiate PKCE flow — Spring generates code_verifier, code_challenge, state, nonce
        final var initResponse = given()
                .redirects().follow(false)
                .when()
                .get("/oauth2/authorization/keycloak")
                .then()
                .statusCode(302)
                .extract().response();

        final String location = initResponse.header("Location");
        final String state = extractQueryParam(location, "state");
        // Spring sends nonce=SHA256(rawNonce) in the URL; we put this hash verbatim in the id_token
        final String nonce = extractQueryParam(location, "nonce");
        final String sessionCookieFromInit = initResponse.cookie("SESSION");

        assertThat(state).isNotBlank();
        assertThat(nonce).isNotBlank();

        // Step 2: build tokens that Keycloak would return after code exchange
        final String issuer = EnvironmentInitializer.getKeycloakBaseUrl() + "/realms/dealership";
        final String accessToken = JwtTestUtils.generateToken("user-pkce", List.of("CLIENT"), "user-pkce@test.com");
        final String refreshToken = "refresh-" + UUID.randomUUID();
        final String idToken = JwtTestUtils.generateIdToken("user-pkce", nonce, issuer, "dealership-bff");

        // Step 3: stub token endpoint
        EnvironmentInitializer.getKeycloakMock().stubFor(
                post(urlPathEqualTo("/realms/dealership/protocol/openid-connect/token"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(buildTokenResponse(accessToken, refreshToken, idToken))));

        // Step 4: stub userinfo endpoint (called by Spring's OidcUserService)
        EnvironmentInitializer.getKeycloakMock().stubFor(
                get(urlPathEqualTo("/realms/dealership/protocol/openid-connect/userinfo"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"sub\":\"user-pkce\",\"email\":\"user-pkce@test.com\",\"preferred_username\":\"user-pkce\"}")));

        // Step 5: simulate callback — must use the session from the init request (holds code_verifier)
        final var callbackResponse = given()
                .redirects().follow(false)
                .cookie("SESSION", sessionCookieFromInit)
                .when()
                .get("/login/oauth2/code/keycloak?code=test-auth-code-" + UUID.randomUUID() + "&state=" + state)
                .then()
                .extract().response();

        // Expect redirect to the frontend with a new SESSION cookie issued
        assertThat(callbackResponse.statusCode()).isIn(302, 200);
        assertThat(callbackResponse.cookie("SESSION")).isNotBlank();
    }

    @Test
    void shouldInjectBearerTokenFromSessionAndReturnApiData() {
        final String accessToken = JwtTestUtils.generateToken(
                "user-session", List.of("CLIENT"), "user-session@test.com");
        final var session = createSession();
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN, accessToken);
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY, Instant.now().plusSeconds(3600));
        saveSession(session);

        EnvironmentInitializer.getClientApiMock().stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/clients/me"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                          "id":"7c3e9f1a-0000-0000-0000-000000000002",
                                          "keycloakId":"user-session",
                                          "firstName":"User",
                                          "lastName":"Session",
                                          "cpf":"52998224725",
                                          "phone":"11999887766",
                                          "createdAt":"2025-01-01T00:00:00Z"
                                        }
                                        """)));

        given()
                .cookie("SESSION", encodeSessionCookie(session.getId()))
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldReturn401WhenSessionTokenIsExpiredAndNoRefreshAvailable() {
        final var session = createSession();
        session.setAttribute(
                OAuth2LoginSuccessHandler.SESSION_ACCESS_TOKEN,
                JwtTestUtils.generateExpiredToken("user-expired"));
        session.setAttribute(
                OAuth2LoginSuccessHandler.SESSION_TOKEN_EXPIRY,
                Instant.now().minusSeconds(3600));
        saveSession(session);

        given()
                .cookie("SESSION", encodeSessionCookie(session.getId()))
                .when()
                .get("/api/v1/profile")
                .then()
                .statusCode(401);
    }

    @SuppressWarnings("unchecked")
    private Session createSession() {
        return (Session) sessionRepository.createSession();
    }

    @SuppressWarnings("unchecked")
    private void saveSession(final Session session) {
        sessionRepository.save(session);
    }

    /** Spring Session's DefaultCookieSerializer uses standard Base64 (not URL-safe) to encode the session ID. */
    private static String encodeSessionCookie(final String sessionId) {
        return Base64.getEncoder().encodeToString(sessionId.getBytes(StandardCharsets.UTF_8));
    }

    private static String extractQueryParam(final String url, final String name) throws Exception {
        final String query = new URI(url).getRawQuery();
        if (query == null) return null;
        for (final String param : query.split("&")) {
            final String[] parts = param.split("=", 2);
            if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String buildTokenResponse(
            final String accessToken, final String refreshToken, final String idToken) {
        return """
                {
                  "access_token": "%s",
                  "refresh_token": "%s",
                  "id_token": "%s",
                  "token_type": "Bearer",
                  "expires_in": 3600,
                  "refresh_expires_in": 86400
                }
                """.formatted(accessToken, refreshToken, idToken);
    }
}
