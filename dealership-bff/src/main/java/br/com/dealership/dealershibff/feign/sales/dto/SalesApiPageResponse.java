package br.com.dealership.dealershibff.feign.sales.dto;

import java.util.List;

public record SalesApiPageResponse<T>(
        List<T> content,
        PageMetadata page,
        Integer size,
        Integer number,
        Long totalElements,
        Integer totalPages
) {
    public SalesApiPageResponse(final List<T> content, final PageMetadata page) {
        this(
                content,
                page,
                page != null ? page.size() : null,
                page != null ? page.number() : null,
                page != null ? page.totalElements() : null,
                page != null ? page.totalPages() : null
        );
    }

    public Integer resolvedSize() {
        return page != null ? page.size() : size;
    }

    public Integer resolvedNumber() {
        return page != null ? page.number() : number;
    }

    public Long resolvedTotalElements() {
        return page != null ? page.totalElements() : totalElements;
    }

    public Integer resolvedTotalPages() {
        return page != null ? page.totalPages() : totalPages;
    }

    public record PageMetadata(int size, int number, long totalElements, int totalPages) {}
}
