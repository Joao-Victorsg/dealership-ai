package br.com.dealership.dealershibff.feign.car.dto;

import java.util.List;

public record CarApiPageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
