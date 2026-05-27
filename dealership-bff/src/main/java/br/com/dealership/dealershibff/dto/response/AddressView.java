package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.feign.client.dto.ClientApiAddressResponse;
import lombok.Builder;

@Builder
public record AddressView(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String cep
) {

    public static AddressView from(final ClientApiAddressResponse source) {
        if (source == null) return null;
        return AddressView.builder()
                .street(source.streetName())
                .number(source.streetNumber())
                .complement(null)
                .neighborhood(null)
                .city(source.city())
                .state(source.state())
                .cep(source.postcode())
                .build();
    }
}
