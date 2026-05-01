package br.com.dealership.dealershibff.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ResponseMeta(
        Instant timestamp,
        String requestId,
        Integer page,
        Integer pageSize,
        Long totalElements,
        Integer totalPages
) {

    public static ResponseMeta of(final String requestId) {
        return ResponseMeta.builder()
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();
    }

    public static ResponseMeta paged(
            final String requestId,
            final int page,
            final int pageSize,
            final long totalElements,
            final int totalPages
    ) {
        return ResponseMeta.builder()
                .timestamp(Instant.now())
                .requestId(requestId)
                .page(page)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
