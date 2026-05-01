package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

import java.util.Collections;
import java.util.List;

public record ErrorBody(
        ErrorCode code,
        String message,
        List<ErrorDetail> details
) {

    public static ErrorBody of(final ErrorCode code, final String message) {
        return new ErrorBody(code, message, Collections.emptyList());
    }

    public static ErrorBody of(final ErrorCode code, final String message, final List<ErrorDetail> details) {
        return new ErrorBody(code, message, Collections.unmodifiableList(details));
    }
}
