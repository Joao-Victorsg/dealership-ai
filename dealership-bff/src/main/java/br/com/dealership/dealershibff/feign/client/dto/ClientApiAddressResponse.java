package br.com.dealership.dealershibff.feign.client.dto;

public record ClientApiAddressResponse(
        String postcode,
        String streetNumber,
        String streetName,
        String city,
        String state,
        boolean addressSearched
) {
}
