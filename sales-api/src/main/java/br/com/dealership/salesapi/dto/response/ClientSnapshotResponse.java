package br.com.dealership.salesapi.dto.response;

import br.com.dealership.salesapi.domain.entity.ClientSnapshot;
import lombok.Builder;

@Builder
public record ClientSnapshotResponse(
        String firstName,
        String lastName,
        String cpf,
        String email,
        AddressSnapshotResponse address
) {
    public static ClientSnapshotResponse from(ClientSnapshot snapshot) {
        return ClientSnapshotResponse.builder()
                .firstName(snapshot.firstName())
                .lastName(snapshot.lastName())
                .cpf(snapshot.cpf())
                .email(snapshot.email())
                .address(AddressSnapshotResponse.from(snapshot.address()))
                .build();
    }
}
