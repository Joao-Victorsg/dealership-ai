package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String cpf,
        String email,
        String phone,
        LocalDateTime createdAt,
        AddressView address
) {

    public static ProfileResponse from(final ClientApiClientResponse source, final String email) {
        return ProfileResponse.builder()
                .id(source.id())
                .firstName(source.firstName() != null ? source.firstName() : "")
                .lastName(source.lastName() != null ? source.lastName() : "")
                .cpf(source.cpf() != null ? source.cpf() : "")
                .email(email)
                .phone(source.phone() != null ? source.phone() : "")
                .createdAt(source.createdAt())
                .address(AddressView.from(source.address()))
                .build();
    }
}
