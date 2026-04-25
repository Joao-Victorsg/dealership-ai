package br.com.dealership.salesapi.domain.entity;

import br.com.dealership.salesapi.dto.request.ClientSnapshotRequest;
import lombok.Builder;

@Builder
public record ClientSnapshot(
        String firstName,
        String lastName,
        String cpf,
        String email,
        AddressSnapshot address
) {
    public static ClientSnapshot from(ClientSnapshotRequest r) {
        return ClientSnapshot.builder()
                .firstName(r.firstName())
                .lastName(r.lastName())
                .cpf(r.cpf())
                .email(r.email())
                .address(AddressSnapshot.from(r.address()))
                .build();
    }
}
