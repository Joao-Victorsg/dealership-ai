package br.com.dealership.dealershibff.feign.car.dto;

/**
 * Envelope that matches the {@code {"data": ...}} wrapper returned by car-api
 * for all single-resource and paginated responses.
 */
public record CarApiDataResponse<T>(T data) {
}
