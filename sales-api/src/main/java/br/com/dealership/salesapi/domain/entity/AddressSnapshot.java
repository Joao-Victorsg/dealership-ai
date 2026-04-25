package br.com.dealership.salesapi.domain.entity;

import br.com.dealership.salesapi.dto.request.AddressSnapshotRequest;
import lombok.Builder;

@Builder
public record AddressSnapshot(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String postcode
) {
    public static AddressSnapshot from(AddressSnapshotRequest r) {
        return AddressSnapshot.builder()
                .street(r.street())
                .number(r.number())
                .complement(r.complement())
                .neighborhood(r.neighborhood())
                .city(r.city())
                .state(r.state())
                .postcode(r.postcode())
                .build();
    }
}
