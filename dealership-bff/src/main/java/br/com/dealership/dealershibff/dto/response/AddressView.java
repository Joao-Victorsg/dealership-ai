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
                .street(source.street())
                .number(source.number())
                .complement(source.complement())
                .neighborhood(source.neighborhood())
                .city(source.city())
                .state(source.state())
                .cep(source.cep())
                .build();
    }
}
