package br.com.dealership.dealershibff.feign.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientApiClientResponse(
        UUID id,
        String keycloakId,
        String firstName,
        String lastName,
        String cpf,
        String phone,
        Instant createdAt,
        Instant deletedAt,
        ClientApiAddressResponse address
) {
}
