package br.com.dealership.dealershibff.feign.car.dto;

import java.util.List;

public record CarApiPageResponse<T>(
        List<T> content,
        PageMetadata page
) {
    public record PageMetadata(int size, int number, long totalElements, int totalPages) {}
}
