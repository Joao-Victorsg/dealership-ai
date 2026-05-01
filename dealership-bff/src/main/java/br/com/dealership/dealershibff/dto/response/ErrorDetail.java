package br.com.dealership.dealershibff.dto.response;

import java.util.List;

public record ErrorDetail(
        String field,
        String reason
) {

    public static ErrorDetail of(final String field, final String reason) {
        return new ErrorDetail(field, reason);
    }
}
