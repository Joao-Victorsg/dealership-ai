package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.dto.request.InventoryFilterRequest;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.InventoryFilterOptionsResponse;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class InventoryService {

    private static final String AVAILABLE_STATUS = "AVAILABLE";

    private final CarApiClient carApiClient;
    private final Executor executor;

    public InventoryService(
            final CarApiClient carApiClient,
            @Qualifier("virtualThreadExecutor") final Executor executor) {
        this.carApiClient = carApiClient;
        this.executor = executor;
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
            final var page = carApiClient.listCars(params).data();
            final var vehicles = page.content().stream()
                    .map(VehicleResponse::from)
                    .toList();
            final var meta = ResponseMeta.paged(
                    getRequestId(),
                    page.page().number(),
                    page.page().size(),
                    page.page().totalElements(),
                    page.page().totalPages()
            );
            return ApiResponse.paged(vehicles, meta);
        }, executor);
    }

    @Cacheable(value = "car-by-id", key = "#carId")
    @CircuitBreaker(name = "car-api")
    @Retry(name = "car-api")
    @RateLimiter(name = "car-api")
    @TimeLimiter(name = "car-api")
    @Bulkhead(name = "car-api")
    public CompletableFuture<ApiResponse<VehicleResponse>> getById(final UUID carId) {
        return CompletableFuture.supplyAsync(() -> {
            final var car = carApiClient.getCarById(carId).data();
            return ApiResponse.of(VehicleResponse.from(car), ResponseMeta.of(getRequestId()));
        }, executor);
    }

    @Cacheable(value = "inventory-filter-options")
    @CircuitBreaker(name = "car-api")
    @Retry(name = "car-api")
    @RateLimiter(name = "car-api")
    @TimeLimiter(name = "car-api")
    @Bulkhead(name = "car-api")
    public CompletableFuture<ApiResponse<InventoryFilterOptionsResponse>> filterOptions() {
        return CompletableFuture.supplyAsync(() -> {
            final var options = carApiClient.getFilterOptions().data();
            final var response = new InventoryFilterOptionsResponse(
                    options.manufacturers(),
                    options.exteriorColors()
            );
            return ApiResponse.of(response, ResponseMeta.of(getRequestId()));
        }, executor);
    }

    private String getRequestId() {
        final String requestId = MDC.get("requestId");
        return requestId != null ? requestId : java.util.UUID.randomUUID().toString();
    }

    private CarApiFilterParams toFilterParams(final InventoryFilterRequest filter) {
        final String normalizedCategory = normalizeCategory(filter.category());
        final String normalizedSortBy = normalizeSortBy(filter.sortBy());
        final String normalizedSortDirection = normalizeSortDirection(filter.sortDirection());
        final String normalizedPropulsionType = normalizePropulsionType(filter.type());
        final Boolean normalizedIsNew = normalizeIsNew(filter.condition());

        return new CarApiFilterParams(
                filter.q(),
                normalizedCategory,
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
                AVAILABLE_STATUS,
                normalizedPropulsionType,
                normalizedIsNew,
                filter.priceMin(),
                filter.priceMax(),
                filter.yearMin(),
                filter.yearMax(),
                normalizedSortBy,
                normalizedSortDirection,
                filter.page(),
                filter.size()
        );
    }

    private String normalizeSortBy(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return null;
        }

        final String value = sortBy.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PRICE", "LISTED_VALUE" -> "LISTED_VALUE";
            case "YEAR", "MANUFACTURING_YEAR" -> "MANUFACTURING_YEAR";
            case "REGISTRATION_DATE", "REGISTRATIONDATE" -> "REGISTRATION_DATE";
            case "MODEL" -> "MODEL";
            case "MANUFACTURER" -> "MANUFACTURER";
            default -> null;
        };
    }

    private String normalizeSortDirection(final String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return null;
        }

        return switch (sortDirection.trim().toUpperCase(Locale.ROOT)) {
            case "ASC", "DESC" -> sortDirection.trim().toUpperCase(Locale.ROOT);
            default -> null;
        };
    }

    private String normalizeCategory(final String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        final String value = category.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "SUV", "SEDAN", "SPORT", "HATCH", "PICKUP" -> value;
            case "HATCHBACK" -> "HATCH";
            default -> null;
        };
    }

    private String normalizePropulsionType(final String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "ELECTRIC" -> "ELECTRIC";
            case "COMBUSTION", "HYBRID", "GASOLINE", "DIESEL" -> "COMBUSTION";
            default -> null;
        };
    }

    private Boolean normalizeIsNew(final String condition) {
        if (condition == null || condition.isBlank()) {
            return null;
        }

        return switch (condition.trim().toUpperCase(Locale.ROOT)) {
            case "NEW" -> true;
            case "USED" -> false;
            default -> null;
        };
    }
}
