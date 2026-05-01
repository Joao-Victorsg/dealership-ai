package br.com.dealership.dealershibff.dto.response;

public record ApiErrorResponse(
        ErrorBody error,
        ResponseMeta meta
) {

    public static ApiErrorResponse of(final ErrorBody error, final ResponseMeta meta) {
        return new ApiErrorResponse(error, meta);
    }
}
