package br.com.dealership.salesapi.dto.response;

public record Response<T>(T data) {

    public static <T> Response<T> of(T data) {
        return new Response<>(data);
    }
}
