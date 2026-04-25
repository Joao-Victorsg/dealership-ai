package br.com.dealership.salesapi.dto.response;

import br.com.dealership.salesapi.domain.entity.AddressSnapshot;
import lombok.Builder;

@Builder
public record AddressSnapshotResponse(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String postcode
) {
    public static AddressSnapshotResponse from(AddressSnapshot snapshot) {
        return AddressSnapshotResponse.builder()
                .street(snapshot.street())
                .number(snapshot.number())
                .complement(snapshot.complement())
                .neighborhood(snapshot.neighborhood())
                .city(snapshot.city())
                .state(snapshot.state())
                .postcode(snapshot.postcode())
                .build();
    }
}
