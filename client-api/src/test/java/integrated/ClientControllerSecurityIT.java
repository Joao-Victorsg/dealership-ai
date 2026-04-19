package integrated;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static integrated.container.ViaCepContainerDefinition.getWireMockClient;
import static integrated.utils.JwtTestUtils.generateTokenFor;
import static io.restassured.RestAssured.given;

class ClientControllerSecurityIT extends BaseIT {

    private String existingProfileId;
    private String existingProfileKeycloakId;

    @BeforeEach
    void setUp() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        existingProfileKeycloakId = UUID.randomUUID().toString();
        final var token = generateTokenFor(existingProfileKeycloakId, "client");

        existingProfileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "Fixture",
                          "lastName": "User",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(existingProfileKeycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");
    }

    // ─── GET /clients/me ──────────────────────────────────────────────────────

    @Test
    void getMyProfileShouldReturn200ForRoleClientWithMatchingSub() {
        final var token = generateTokenFor(existingProfileKeycloakId, "client");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/clients/me")
        .then()
                .statusCode(200);
    }

    @Test
    void getMyProfileShouldReturn403WhenNoProfileExistsForSub() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/clients/me")
        .then()
                .statusCode(403);
    }

    @Test
    void getMyProfileShouldReturn403ForRoleStaff() {
        final var token = generateTokenFor(existingProfileKeycloakId, "staff");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/clients/me")
        .then()
                .statusCode(403);
    }

    @Test
    void getMyProfileShouldReturn403ForRoleAdmin() {
        final var token = generateTokenFor(existingProfileKeycloakId, "admin");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/clients/me")
        .then()
                .statusCode(403);
    }

    // ─── PATCH /clients/{id} ─────────────────────────────────────────────────

    @Test
    void patchClientShouldReturn200ForRoleClientWithOwnProfile() {
        final var token = generateTokenFor(existingProfileKeycloakId, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"firstName\": \"Updated\"}")
        .when()
                .patch("/clients/" + existingProfileId)
        .then()
                .statusCode(200);
    }

    @Test
    void patchClientShouldReturn403ForRoleClientWithCrossProfile() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"firstName\": \"Hacked\"}")
        .when()
                .patch("/clients/" + existingProfileId)
        .then()
                .statusCode(403);
    }

    @Test
    void patchClientShouldReturn200ForRoleSystemRegardlessOfOwnership() {
        final var systemToken = generateTokenFor(UUID.randomUUID().toString(), "system");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + systemToken)
                .body("{\"firstName\": \"System\"}")
        .when()
                .patch("/clients/" + existingProfileId)
        .then()
                .statusCode(200);
    }

    @Test
    void patchClientShouldReturn403ForRoleStaff() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "staff");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"firstName\": \"Attempt\"}")
        .when()
                .patch("/clients/" + existingProfileId)
        .then()
                .statusCode(403);
    }

    @Test
    void patchClientShouldReturn403ForRoleAdmin() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "admin");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"firstName\": \"Attempt\"}")
        .when()
                .patch("/clients/" + existingProfileId)
        .then()
                .statusCode(403);
    }

    @Test
    void patchClientShouldReturn401WithNoToken() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"firstName\": \"Attempt\"}")
        .when()
                .patch("/clients/" + existingProfileId)
        .then()
                .statusCode(401);
    }

    // ─── PATCH /clients/{id}/cpf ──────────────────────────────────────────────

    @Test
    void patchCpfShouldReturn200ForRoleAdmin() {
        final var adminToken = generateTokenFor(UUID.randomUUID().toString(), "admin");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("{\"cpf\": \"111.444.777-35\"}")
        .when()
                .patch("/clients/" + existingProfileId + "/cpf")
        .then()
                .statusCode(200);
    }

    @Test
    void patchCpfShouldReturn403ForRoleClient() {
        final var token = generateTokenFor(existingProfileKeycloakId, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"cpf\": \"111.444.777-35\"}")
        .when()
                .patch("/clients/" + existingProfileId + "/cpf")
        .then()
                .statusCode(403);
    }

    @Test
    void patchCpfShouldReturn403ForRoleStaff() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "staff");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"cpf\": \"111.444.777-35\"}")
        .when()
                .patch("/clients/" + existingProfileId + "/cpf")
        .then()
                .statusCode(403);
    }

    @Test
    void patchCpfShouldReturn403ForRoleSystem() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "system");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"cpf\": \"111.444.777-35\"}")
        .when()
                .patch("/clients/" + existingProfileId + "/cpf")
        .then()
                .statusCode(403);
    }

    @Test
    void patchCpfShouldReturn401WithNoToken() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"cpf\": \"111.444.777-35\"}")
        .when()
                .patch("/clients/" + existingProfileId + "/cpf")
        .then()
                .statusCode(401);
    }

    // ─── DELETE /clients/{id} ─────────────────────────────────────────────────

    @Test
    void deleteClientShouldReturn204ForRoleClientOwnProfile() {
        final var token = generateTokenFor(existingProfileKeycloakId, "client");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/clients/" + existingProfileId)
        .then()
                .statusCode(204);
    }

    @Test
    void deleteClientShouldReturn403ForRoleClientCrossProfile() {
        final var attackerToken = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .header("Authorization", "Bearer " + attackerToken)
        .when()
                .delete("/clients/" + existingProfileId)
        .then()
                .statusCode(403);
    }

    @Test
    void deleteClientShouldReturn403ForRoleStaff() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "staff");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/clients/" + existingProfileId)
        .then()
                .statusCode(403);
    }

    @Test
    void deleteClientShouldReturn403ForRoleAdmin() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "admin");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/clients/" + existingProfileId)
        .then()
                .statusCode(403);
    }

    @Test
    void deleteClientShouldReturn401WithNoToken() {
        given()
        .when()
                .delete("/clients/" + existingProfileId)
        .then()
                .statusCode(401);
    }
}
