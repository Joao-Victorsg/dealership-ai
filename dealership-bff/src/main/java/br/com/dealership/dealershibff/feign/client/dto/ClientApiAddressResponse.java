package br.com.dealership.dealershibff.feign.client.dto;

public record ClientApiAddressResponse(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String cep
) {
}
