package br.com.dealership.dealershibff.feign.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientApiClientResponse(
        UUID id,
        String keycloakId,
        String firstName,
        String lastName,
        String cpf,
        @JsonAlias({"phone", "phoneNumber"})
        String phone,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        ClientApiAddressResponse address
) {
}
