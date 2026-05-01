package br.com.dealership.dealershibff.feign.sales.dto;

import lombok.Builder;

@Builder
public record SalesApiClientSnapshot(
        String firstName,
        String lastName,
        String cpf,
        String email,
        String phone
) {
}
