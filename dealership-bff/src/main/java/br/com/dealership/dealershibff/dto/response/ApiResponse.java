package br.com.dealership.dealershibff.dto.response;

public record ApiResponse<T>(
        T data,
        ResponseMeta meta
) {

    public static <T> ApiResponse<T> of(final T data, final ResponseMeta meta) {
        return new ApiResponse<>(data, meta);
    }

    public static <T> ApiResponse<T> paged(final T data, final ResponseMeta meta) {
        return new ApiResponse<>(data, meta);
    }
}
