package integrated;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static integrated.container.ViaCepContainerDefinition.getWireMockClient;
import static integrated.utils.JwtTestUtils.generateTokenFor;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

class ClientControllerIT extends BaseIT {

    // ─── POST /clients ────────────────────────────────────────────────────────

    @Test
    void postClientsShouldReturn201WithResolvedAddressWhenViaCepSucceeds() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {"cep":"01310-100","logradouro":"Avenida Paulista","localidade":"São Paulo","uf":"SP"}
                                        """)));

        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "100"
                        }
                        """.formatted(UUID.randomUUID()))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.firstName", equalTo("João"))
                .body("data.address.addressSearched", equalTo(true))
                .body("data.address.city", equalTo("São Paulo"))
                .body("data.createdAt", notNullValue());
    }

    @Test
    void postClientsShouldReturn201WithAddressSearchedFalseWhenViaCepUnavailable() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");
        final var keycloakId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "99999-999",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .body("data.address.addressSearched", equalTo(false));
    }

    @Test
    void postClientsShouldReturn422WhenDuplicateCpf() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var token1 = generateTokenFor(UUID.randomUUID().toString(), "client");
        final var keycloakId1 = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token1)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId1))
        .when()
                .post("/clients")
        .then()
                .statusCode(201);

        final var token2 = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token2)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "Maria",
                          "lastName": "Souza",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 88888-8888",
                          "postcode": "01310-100",
                          "streetNumber": "2"
                        }
                        """.formatted(UUID.randomUUID()))
        .when()
                .post("/clients")
        .then()
                .statusCode(422);
    }

    @Test
    void postClientsShouldReturn422WhenDuplicateKeycloakId() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var token = generateTokenFor(keycloakId, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201);

        final var token2 = generateTokenFor(keycloakId, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token2)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "Maria",
                          "lastName": "Souza",
                          "cpf": "111.444.777-35",
                          "phoneNumber": "+55 11 88888-8888",
                          "postcode": "01310-100",
                          "streetNumber": "2"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(422);
    }

    @Test
    void postClientsShouldReturn400WhenCpfIsInvalid() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "000.000.000-00",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(UUID.randomUUID()))
        .when()
                .post("/clients")
        .then()
                .statusCode(400);
    }

    @Test
    void postClientsShouldReturn400WhenPhoneNumberIsInvalid() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "invalid-phone",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(UUID.randomUUID()))
        .when()
                .post("/clients")
        .then()
                .statusCode(400);
    }

    // ─── GET /clients/me ──────────────────────────────────────────────────────

    @Test
    void getMyProfileShouldReturn200WithProfileForAuthenticatedClient() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var createToken = generateTokenFor(keycloakId, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + createToken)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201);

        final var getToken = generateTokenFor(keycloakId, "client");

        given()
                .header("Authorization", "Bearer " + getToken)
        .when()
                .get("/clients/me")
        .then()
                .statusCode(200)
                .body("data.firstName", equalTo("João"))
                .body("data.cpf", nullValue())
                .body("data.keycloakId", nullValue());
    }

    @Test
    void getMyProfileShouldReturn401WhenUnauthenticated() {
        given()
        .when()
                .get("/clients/me")
        .then()
                .statusCode(401);
    }

    @Test
    void getMyProfileShouldReturn403ForRoleStaff() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "staff");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/clients/me")
        .then()
                .statusCode(403);
    }

    // ─── PATCH /clients/{id} ─────────────────────────────────────────────────

    @Test
    void patchClientShouldReturn200AndUpdatePersonalFieldsForRoleClient() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var token = generateTokenFor(keycloakId, "client");

        final var id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var patchToken = generateTokenFor(keycloakId, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + patchToken)
                .body("""
                        {"firstName": "Updated", "lastName": "Name"}
                        """)
        .when()
                .patch("/clients/" + id)
        .then()
                .statusCode(200)
                .body("data.firstName", equalTo("Updated"))
                .body("data.lastName", equalTo("Name"));
    }

    @Test
    void patchClientShouldReturn400WhenOnlyPostcodeProvidedWithoutStreetNumber() {
        final var token = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"postcode": "01310-100"}
                        """)
        .when()
                .patch("/clients/" + UUID.randomUUID())
        .then()
                .statusCode(400);
    }

    @Test
    void patchClientShouldReturn403WhenRoleClientTriesCrossClientUpdate() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var ownerId = UUID.randomUUID().toString();
        final var ownerToken = generateTokenFor(ownerId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "Owner",
                          "lastName": "User",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(ownerId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var attackerToken = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + attackerToken)
                .body("""
                        {"firstName": "Hacked"}
                        """)
        .when()
                .patch("/clients/" + profileId)
        .then()
                .statusCode(403);
    }

    @Test
    void patchClientShouldReturn422WhenProfileIsInactive() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var token = generateTokenFor(keycloakId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var deleteToken = generateTokenFor(keycloakId, "client");
        given()
                .header("Authorization", "Bearer " + deleteToken)
        .when()
                .delete("/clients/" + profileId)
        .then()
                .statusCode(204);

        final var patchToken = generateTokenFor(keycloakId, "client");
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + patchToken)
                .body("""
                        {"firstName": "New"}
                        """)
        .when()
                .patch("/clients/" + profileId)
        .then()
                .statusCode(422);
    }

    // ─── PATCH /clients/{id}/cpf ──────────────────────────────────────────────

    @Test
    void patchCpfShouldReturn200ForRoleAdmin() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var clientToken = generateTokenFor(keycloakId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + clientToken)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var adminToken = generateTokenFor(UUID.randomUUID().toString(), "admin");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("""
                        {"cpf": "111.444.777-35"}
                        """)
        .when()
                .patch("/clients/" + profileId + "/cpf")
        .then()
                .statusCode(200)
                .body("data.cpf", nullValue());
    }

    @Test
    void patchCpfShouldReturn422WhenDuplicateCpf() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId1 = UUID.randomUUID().toString();
        final var keycloakId2 = UUID.randomUUID().toString();
        final var token1 = generateTokenFor(keycloakId1, "client");
        final var token2 = generateTokenFor(keycloakId2, "client");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token1)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId1))
        .when()
                .post("/clients").then().statusCode(201);

        final var profileId2 = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token2)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "Maria",
                          "lastName": "Souza",
                          "cpf": "111.444.777-35",
                          "phoneNumber": "+55 11 88888-8888",
                          "postcode": "01310-100",
                          "streetNumber": "2"
                        }
                        """.formatted(keycloakId2))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var adminToken = generateTokenFor(UUID.randomUUID().toString(), "admin");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("""
                        {"cpf": "529.982.247-25"}
                        """)
        .when()
                .patch("/clients/" + profileId2 + "/cpf")
        .then()
                .statusCode(422);
    }

    @Test
    void patchCpfShouldReturn400WhenCpfIsInvalid() {
        final var adminToken = generateTokenFor(UUID.randomUUID().toString(), "admin");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body("""
                        {"cpf": "000.000.000-00"}
                        """)
        .when()
                .patch("/clients/" + UUID.randomUUID() + "/cpf")
        .then()
                .statusCode(400);
    }

    // ─── DELETE /clients/{id} ─────────────────────────────────────────────────

    @Test
    void deleteClientShouldReturn204AndAnonymizeProfile() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var token = generateTokenFor(keycloakId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var deleteToken = generateTokenFor(keycloakId, "client");

        given()
                .header("Authorization", "Bearer " + deleteToken)
        .when()
                .delete("/clients/" + profileId)
        .then()
                .statusCode(204);
    }

    @Test
    void deleteClientSubsequentPatchShouldReturn422() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var keycloakId = UUID.randomUUID().toString();
        final var token = generateTokenFor(keycloakId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(keycloakId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var deleteToken = generateTokenFor(keycloakId, "client");
        given()
                .header("Authorization", "Bearer " + deleteToken)
        .when()
                .delete("/clients/" + profileId)
        .then()
                .statusCode(204);

        final var patchToken = generateTokenFor(keycloakId, "client");
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + patchToken)
                .body("""
                        {"firstName": "Recovered"}
                        """)
        .when()
                .patch("/clients/" + profileId)
        .then()
                .statusCode(422);
    }

    @Test
    void deleteClientShouldReturn403WhenCrossClientDelete() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var ownerId = UUID.randomUUID().toString();
        final var ownerToken = generateTokenFor(ownerId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "Owner",
                          "lastName": "User",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(ownerId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .extract().path("data.id");

        final var attackerToken = generateTokenFor(UUID.randomUUID().toString(), "client");

        given()
                .header("Authorization", "Bearer " + attackerToken)
        .when()
                .delete("/clients/" + profileId)
        .then()
                .statusCode(403);
    }

    // ─── ROLE_SYSTEM address retry ────────────────────────────────────────────

    @Test
    void patchClientWithRoleSystemShouldResolveAddressWhenViaCepAvailable() {
        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse().withStatus(503)));

        final var ownerId = UUID.randomUUID().toString();
        final var ownerToken = generateTokenFor(ownerId, "client");

        final var profileId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .body("""
                        {
                          "keycloakId": "%s",
                          "firstName": "João",
                          "lastName": "Silva",
                          "cpf": "529.982.247-25",
                          "phoneNumber": "+55 11 99999-9999",
                          "postcode": "01310-100",
                          "streetNumber": "1"
                        }
                        """.formatted(ownerId))
        .when()
                .post("/clients")
        .then()
                .statusCode(201)
                .body("data.address.addressSearched", equalTo(false))
                .extract().path("data.id");

        getWireMockClient().register(
                get(urlPathMatching("/ws/.+/json/")).atPriority(1)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {"cep":"01310-100","logradouro":"Avenida Paulista","localidade":"São Paulo","uf":"SP"}
                                        """)));

        final var systemToken = generateTokenFor(UUID.randomUUID().toString(), "system");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + systemToken)
                .body("""
                        {"postcode": "01310-100", "streetNumber": "1"}
                        """)
        .when()
                .patch("/clients/" + profileId)
        .then()
                .statusCode(200)
                .body("data.address.addressSearched", equalTo(true))
                .body("data.address.city", equalTo("São Paulo"));
    }
}
