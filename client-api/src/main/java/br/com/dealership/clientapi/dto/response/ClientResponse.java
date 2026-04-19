package br.com.dealership.clientapi.dto.response;

import br.com.dealership.clientapi.entity.Client;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Schema(description = "Client profile representation")
public record ClientResponse(
        @Schema(description = "Unique identifier") UUID id,
        @Schema(description = "First name") String firstName,
        @Schema(description = "Last name") String lastName,
        @Schema(description = "Brazilian phone number") String phoneNumber,
        @Schema(description = "Residential address") AddressResponse address,
        @Schema(description = "Timestamp when the profile was created") LocalDateTime createdAt,
        @Schema(description = "Timestamp when the profile was anonymized; null if active") LocalDateTime deletedAt
) {

    public static ClientResponse from(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .phoneNumber(client.getPhoneNumber())
                .address(AddressResponse.from(client.getAddress()))
                .createdAt(client.getCreatedAt())
                .deletedAt(client.getDeletedAt())
                .build();
    }
}
