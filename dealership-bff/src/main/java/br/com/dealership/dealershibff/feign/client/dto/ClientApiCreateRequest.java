package br.com.dealership.dealershibff.feign.client.dto;

import lombok.Builder;

@Builder
public record ClientApiCreateRequest(
        String keycloakId,
        String firstName,
        String lastName,
        String cpf,
        String phoneNumber,
        String postcode,
        String streetNumber
) {
}
