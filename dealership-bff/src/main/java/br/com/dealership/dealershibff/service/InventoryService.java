package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.dto.request.InventoryFilterRequest;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import br.com.dealership.dealershibff.dto.response.VehicleResponse;
import br.com.dealership.dealershibff.feign.car.CarApiClient;
import br.com.dealership.dealershibff.feign.car.dto.CarApiFilterParams;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class InventoryService {

    private final CarApiClient carApiClient;

    public InventoryService(final CarApiClient carApiClient) {
        this.carApiClient = carApiClient;
    }

    @Cacheable(value = "car-listings", key = "#filter.toCacheKey()")
    @CircuitBreaker(name = "car-api")
    @Retry(name = "car-api")
    @RateLimiter(name = "car-api")
    @TimeLimiter(name = "car-api")
    @Bulkhead(name = "car-api")
    public CompletableFuture<ApiResponse<List<VehicleResponse>>> list(final InventoryFilterRequest filter) {
        return CompletableFuture.supplyAsync(() -> {
            final var params = toFilterParams(filter);
            final var page = carApiClient.listCars(params);
            final var vehicles = page.content().stream()
                    .map(VehicleResponse::from)
                    .toList();
            final var meta = ResponseMeta.paged(
                    getRequestId(),
                    page.number(),
                    page.size(),
                    page.totalElements(),
                    page.totalPages()
            );
            return ApiResponse.paged(vehicles, meta);
        });
    }

    @Cacheable(value = "car-by-id", key = "#carId")
    @CircuitBreaker(name = "car-api")
    @Retry(name = "car-api")
    @RateLimiter(name = "car-api")
    @TimeLimiter(name = "car-api")
    @Bulkhead(name = "car-api")
    public CompletableFuture<ApiResponse<VehicleResponse>> getById(final UUID carId) {
        return CompletableFuture.supplyAsync(() -> {
            final var car = carApiClient.getCarById(carId);
            return ApiResponse.of(VehicleResponse.from(car), ResponseMeta.of(getRequestId()));
        });
    }

    private String getRequestId() {
        final String requestId = MDC.get("requestId");
        return requestId != null ? requestId : java.util.UUID.randomUUID().toString();
    }

    private CarApiFilterParams toFilterParams(final InventoryFilterRequest filter) {
        return new CarApiFilterParams(
                filter.q(),
                filter.category(),
                filter.type(),
                filter.condition(),
                filter.manufacturer(),
                filter.yearMin(),
                filter.yearMax(),
                filter.priceMin(),
                filter.priceMax(),
                filter.color(),
                filter.kmMin(),
                filter.kmMax(),
                filter.sortBy(),
                filter.sortDirection(),
                filter.page(),
                filter.size()
        );
    }
}
