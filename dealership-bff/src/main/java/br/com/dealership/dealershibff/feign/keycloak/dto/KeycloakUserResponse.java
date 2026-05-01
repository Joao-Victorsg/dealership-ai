package br.com.dealership.dealershibff.feign.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserResponse(String id, String username, String email) {
}
