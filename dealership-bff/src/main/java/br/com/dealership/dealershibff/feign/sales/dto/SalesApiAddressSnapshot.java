package br.com.dealership.dealershibff.feign.sales.dto;

import lombok.Builder;

@Builder
public record SalesApiAddressSnapshot(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String postcode
) {
}
