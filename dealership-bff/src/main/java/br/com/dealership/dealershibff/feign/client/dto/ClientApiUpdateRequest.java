package br.com.dealership.dealershibff.feign.client.dto;

public record ClientApiUpdateRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String postcode,
        String streetNumber
) {
}
