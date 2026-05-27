package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.PurchaseResponse;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import br.com.dealership.dealershibff.feign.car.CarApiClient;
import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.sales.SalesApiClient;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiAddressSnapshot;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiCarSnapshot;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiClientSnapshot;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiRegisterRequest;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiSaleResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class PurchaseService {

    private final CarApiClient carApiClient;
    private final ClientApiClient clientApiClient;
    private final SalesApiClient salesApiClient;
    private final Executor executor;

    public PurchaseService(
            final CarApiClient carApiClient,
            final ClientApiClient clientApiClient,
            final SalesApiClient salesApiClient,
            @Qualifier("virtualThreadExecutor") final Executor executor) {
        this.carApiClient = carApiClient;
        this.clientApiClient = clientApiClient;
        this.salesApiClient = salesApiClient;
        this.executor = executor;
    }

    @CacheEvict(value = "car-by-id", key = "#carId")
    @CircuitBreaker(name = "sales-api")
    @RateLimiter(name = "sales-api")
    @TimeLimiter(name = "sales-api")
    @Bulkhead(name = "sales-api")
    public CompletableFuture<PurchaseResponse> purchase(
            final UUID carId,
            final String bearerToken,
            final String emailFromJwt,
            final UUID clientId) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: availability check
            final var availability = carApiClient.getCarById(carId).data();
            if (!"AVAILABLE".equalsIgnoreCase(availability.status())) {
                throw new CarNotAvailableException("Car " + carId + " is not available for purchase");
            }

            // Step 2: parallel fetch of full car data + client profile
            final CompletableFuture<CarApiCarResponse> carFuture =
                    CompletableFuture.supplyAsync(() -> carApiClient.getCarById(carId).data(), executor);
            final CompletableFuture<ClientApiClientResponse> clientFuture =
                    CompletableFuture.supplyAsync(() -> clientApiClient.getMe(bearerToken).data(), executor);

            final var all = CompletableFuture.allOf(carFuture, clientFuture);
            all.exceptionally(ex -> {
                carFuture.cancel(true);
                clientFuture.cancel(true);
                return null;
            });
            try {
                all.join();
            } catch (final Exception ex) {
                throw new DownstreamServiceException("Failed to fetch car or client data: " + ex.getMessage());
            }

            final var car = carFuture.join();
            final var client = clientFuture.join();

            // Step 3: assemble sale payload
            final var carSnapshot = SalesApiCarSnapshot.builder()
                    .model(valueOrFallback(car.model(), "N/A"))
                    .manufacturer(valueOrFallback(car.manufacturer(), "N/A"))
                    .externalColor(valueOrFallback(car.externalColor(), "N/A"))
                    .internalColor(valueOrFallback(car.internalColor(), "N/A"))
                    .manufacturingYear(car.manufacturingYear())
                    .optionalItems(car.optionalItems())
                    .type(valueOrFallback(car.type(), "N/A"))
                    .category(valueOrFallback(car.category(), "N/A"))
                    .vin(normalizeVin(car.vin()))
                    .listedValue(car.listedValue())
                    .status(valueOrFallback(car.status(), "AVAILABLE").toUpperCase(Locale.ROOT))
                    .build();
            final var addressSnapshot = toAddressSnapshot(client);
            final var clientSnapshot = SalesApiClientSnapshot.builder()
                    .firstName(client.firstName())
                    .lastName(client.lastName())
                    .cpf(normalizeCpf(client.cpf()))
                    .email(emailFromJwt)
                    .address(addressSnapshot)
                    .build();
            final var request = SalesApiRegisterRequest.of(carId, clientId, clientSnapshot, carSnapshot);

            // Step 4: submit sale (no @Retry)
            final var sale = salesApiClient.registerSale(bearerToken, request).data();
            return PurchaseResponse.from(sale);
        }, executor);
    }

    @CircuitBreaker(name = "sales-api")
    @Retry(name = "sales-api")
    @RateLimiter(name = "sales-api")
    @TimeLimiter(name = "sales-api")
    @Bulkhead(name = "sales-api")
    public CompletableFuture<ApiResponse<List<PurchaseResponse>>> history(
            final String bearerToken,
            final int page,
            final int size,
            final Instant from,
            final Instant to,
            final String requestId) {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("size", size);
            if (from != null) params.put("from", from.toString());
            if (to != null) params.put("to", to.toString());

            final var pageResponse = salesApiClient.listSales(bearerToken, params).data();
            if (pageResponse == null) {
                throw new DownstreamServiceException("Sales API returned invalid history payload");
            }
            final Integer pageNumber = pageResponse.resolvedNumber();
            final Integer pageSize = pageResponse.resolvedSize();
            final Long totalElements = pageResponse.resolvedTotalElements();
            final Integer totalPages = pageResponse.resolvedTotalPages();
            if (pageNumber == null || pageSize == null || totalElements == null || totalPages == null) {
                throw new DownstreamServiceException("Sales API returned invalid history payload");
            }
            final var items = pageResponse.content() == null
                    ? List.<PurchaseResponse>of()
                    : pageResponse.content().stream().map(PurchaseResponse::from).toList();

            final var meta = ResponseMeta.paged(
                    requestId,
                    pageNumber,
                    pageSize,
                    totalElements,
                    totalPages
            );
            return ApiResponse.of(items, meta);
        }, executor);
    }

    private SalesApiAddressSnapshot toAddressSnapshot(final ClientApiClientResponse client) {
        final var address = client.address();
        if (address == null) {
            throw new IllegalArgumentException("Client profile address is required to complete purchase");
        }

        return SalesApiAddressSnapshot.builder()
                .street(valueOrFallback(address.streetName(), "N/A"))
                .number(valueOrFallback(address.streetNumber(), "N/A"))
                .complement(null)
                .neighborhood(valueOrFallback(address.city(), "N/A"))
                .city(valueOrFallback(address.city(), "N/A"))
                .state(valueOrFallback(address.state(), "N/A"))
                .postcode(valueOrFallback(address.postcode(), "N/A"))
                .build();
    }

    private String valueOrFallback(final String value, final String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String normalizeCpf(final String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("\\D", "");
    }

    private String normalizeVin(final String vin) {
        if (vin == null) {
            throw new IllegalArgumentException("Selected car has invalid VIN in catalog");
        }
        final String normalized = vin.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalized.length() != 17) {
            throw new IllegalArgumentException("Selected car has invalid VIN in catalog");
        }
        return normalized;
    }
}
