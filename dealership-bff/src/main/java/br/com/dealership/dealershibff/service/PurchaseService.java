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
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiClientSnapshot;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiRegisterRequest;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiSaleResponse;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiVehicleSnapshot;
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
    public CompletableFuture<PurchaseResponse> purchase(final UUID carId, final String bearerToken, final String emailFromJwt) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: availability check
            final var availability = carApiClient.getCarById(carId);
            if (!"AVAILABLE".equalsIgnoreCase(availability.status())) {
                throw new CarNotAvailableException("Car " + carId + " is not available for purchase");
            }

            // Step 2: parallel fetch of full car data + client profile
            final CompletableFuture<CarApiCarResponse> carFuture =
                    CompletableFuture.supplyAsync(() -> carApiClient.getCarById(carId), executor);
            final CompletableFuture<ClientApiClientResponse> clientFuture =
                    CompletableFuture.supplyAsync(() -> clientApiClient.getMe(bearerToken), executor);

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
            final var vehicleSnapshot = SalesApiVehicleSnapshot.builder()
                    .id(car.id())
                    .model(car.model())
                    .manufacturer(car.manufacturer())
                    .manufacturingYear(car.manufacturingYear())
                    .externalColor(car.externalColor())
                    .vin(car.vin())
                    .category(car.category())
                    .listedValue(car.listedValue())
                    .build();
            final var clientSnapshot = SalesApiClientSnapshot.builder()
                    .firstName(client.firstName())
                    .lastName(client.lastName())
                    .cpf(client.cpf())
                    .email(emailFromJwt)
                    .phone(client.phone())
                    .build();
            final var request = SalesApiRegisterRequest.of(carId, vehicleSnapshot, clientSnapshot, "STANDARD_PURCHASE");

            // Step 4: submit sale (no @Retry)
            final var sale = salesApiClient.registerSale(request);
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

            final var pageResponse = salesApiClient.listSales(bearerToken, params);
            final var items = pageResponse.content() == null
                    ? List.<PurchaseResponse>of()
                    : pageResponse.content().stream().map(PurchaseResponse::from).toList();

            final var meta = ResponseMeta.paged(
                    requestId,
                    pageResponse.number(),
                    pageResponse.size(),
                    pageResponse.totalElements(),
                    pageResponse.totalPages()
            );
            return ApiResponse.of(items, meta);
        }, executor);
    }
}
