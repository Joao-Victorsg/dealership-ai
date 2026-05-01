package br.com.dealership.dealershibff.feign.sales.dto;

import java.util.List;

public record SalesApiPageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
