package br.com.dealership.dealershibff.feign.keycloak;

import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakCreateUserRequest;
import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakTokenResponse;
import br.com.dealership.dealershibff.feign.keycloak.dto.KeycloakUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "keycloak",
        url = "${spring.cloud.openfeign.client.config.keycloak.url}",
        configuration = KeycloakFeignConfig.class
)
public interface KeycloakClient {

    @PostMapping(
            value = "/realms/${keycloak.realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KeycloakTokenResponse login(@RequestBody MultiValueMap<String, String> formBody);

    @PostMapping(
            value = "/realms/${keycloak.realm}/protocol/openid-connect/logout",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    void logout(@RequestBody MultiValueMap<String, String> formBody);

    @PostMapping(
            value = "/admin/realms/${keycloak.realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void createUser(
            @RequestHeader("Authorization") String adminToken,
            @RequestBody KeycloakCreateUserRequest request
    );

    @GetMapping("/admin/realms/${keycloak.realm}/users")
    List<KeycloakUserResponse> searchUsers(
            @RequestHeader("Authorization") String adminToken,
            @RequestParam("email") String email,
            @RequestParam("exact") boolean exact
    );

    @DeleteMapping("/admin/realms/${keycloak.realm}/users/{userId}")
    void deleteUser(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable String userId
    );

    @PutMapping("/admin/realms/${keycloak.realm}/users/{userId}/send-verify-email")
    void sendVerifyEmail(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable String userId
    );
}
