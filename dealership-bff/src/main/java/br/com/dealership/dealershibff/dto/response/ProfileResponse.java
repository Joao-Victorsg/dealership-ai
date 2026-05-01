package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String cpf,
        String email,
        String phone,
        Instant createdAt,
        AddressView address
) {

    public static ProfileResponse from(final ClientApiClientResponse source, final String email) {
        return ProfileResponse.builder()
                .id(source.id())
                .firstName(source.firstName())
                .lastName(source.lastName())
                .cpf(source.cpf())
                .email(email)
                .phone(source.phone())
                .createdAt(source.createdAt())
                .address(AddressView.from(source.address()))
                .build();
    }
}
