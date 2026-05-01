package br.com.dealership.dealershibff.feign.keycloak.dto;

import java.util.List;

public record KeycloakCreateUserRequest(
        String username,
        String email,
        boolean enabled,
        List<KeycloakCredential> credentials
) {

    public static KeycloakCreateUserRequest of(final String email, final String password) {
        return new KeycloakCreateUserRequest(
                email,
                email,
                true,
                List.of(KeycloakCredential.password(password))
        );
    }
}
