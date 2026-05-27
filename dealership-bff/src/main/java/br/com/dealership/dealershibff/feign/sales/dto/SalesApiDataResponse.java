package br.com.dealership.dealershibff.feign.sales.dto;

public record SalesApiDataResponse<T>(
        T data
) {
}
