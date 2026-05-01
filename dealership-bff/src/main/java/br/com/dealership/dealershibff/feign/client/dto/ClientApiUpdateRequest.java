package br.com.dealership.dealershibff.feign.client.dto;

public record ClientApiUpdateRequest(
        String firstName,
        String lastName,
        String phone,
        String cep
) {
}
