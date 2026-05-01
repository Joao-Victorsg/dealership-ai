package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.feign.car.CarApiClient;
import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.sales.SalesApiClient;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiPageResponse;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiSaleResponse;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private CarApiClient carApiClient;

    @Mock
    private ClientApiClient clientApiClient;

    @Mock
    private SalesApiClient salesApiClient;

    private PurchaseService purchaseService;

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(carApiClient, clientApiClient, salesApiClient, DIRECT_EXECUTOR);
    }

    @Test
    void shouldReturnPurchaseResponseOnSuccessfulPurchase() throws Exception {
        final var carId = UUID.randomUUID();
        final var car = buildAvailableCar(carId);
        final var client = Instancio.create(ClientApiClientResponse.class);
        final var saleResponse = Instancio.create(SalesApiSaleResponse.class);

        when(carApiClient.getCarById(carId)).thenReturn(car);
        when(clientApiClient.getMe(anyString())).thenReturn(client);
        when(salesApiClient.registerSale(any())).thenReturn(saleResponse);

        final var result = purchaseService.purchase(carId, "Bearer token", "user@test.com").get();

        assertNotNull(result);
        assertEquals(saleResponse.id(), result.id());
        verify(salesApiClient, times(1)).registerSale(any());
    }

    @Test
    void shouldThrowCarNotAvailableExceptionWhenCarStatusIsNotAvailable() {
        final var carId = UUID.randomUUID();
        final var car = buildCarWithStatus(carId, "SOLD");
        when(carApiClient.getCarById(carId)).thenReturn(car);

        final var future = purchaseService.purchase(carId, "Bearer token", "user@test.com");
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(CarNotAvailableException.class, ex.getCause().getClass());
        verify(salesApiClient, never()).registerSale(any());
    }

    @Test
    void shouldThrowCarNotAvailableWhenSalesApiReturns409AndCallExactlyOnce() {
        final var carId = UUID.randomUUID();
        final var car = buildAvailableCar(carId);
        final var client = Instancio.create(ClientApiClientResponse.class);

        when(carApiClient.getCarById(carId)).thenReturn(car);
        when(clientApiClient.getMe(anyString())).thenReturn(client);
        when(salesApiClient.registerSale(any())).thenThrow(new CarNotAvailableException("Already sold"));

        final var future = purchaseService.purchase(carId, "Bearer token", "user@test.com");
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(CarNotAvailableException.class, ex.getCause().getClass());
        verify(salesApiClient, times(1)).registerSale(any());
    }

    @Test
    void shouldThrowDownstreamServiceExceptionWhenSalesApiReturns5xx() {
        final var carId = UUID.randomUUID();
        final var car = buildAvailableCar(carId);
        final var client = Instancio.create(ClientApiClientResponse.class);

        when(carApiClient.getCarById(carId)).thenReturn(car);
        when(clientApiClient.getMe(anyString())).thenReturn(client);
        when(salesApiClient.registerSale(any())).thenThrow(new DownstreamServiceException("unavailable"));

        final var future = purchaseService.purchase(carId, "Bearer token", "user@test.com");
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
        verify(salesApiClient, times(1)).registerSale(any());
    }

    @Test
    void shouldReturnPaginatedHistoryWithCorrectMeta() throws Exception {
        final var sale = Instancio.create(SalesApiSaleResponse.class);
        final var pageResponse = new SalesApiPageResponse<>(List.of(sale), 1L, 1, 0, 20);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(pageResponse);

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-123").get();

        assertNotNull(result);
        assertEquals(1, result.data().size());
        assertEquals(1L, result.meta().totalElements());
    }

    @Test
    void shouldReturnEmptyListWhenNoHistory() throws Exception {
        final var pageResponse = new SalesApiPageResponse<SalesApiSaleResponse>(List.of(), 0L, 0, 0, 20);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(pageResponse);

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-123").get();

        assertNotNull(result);
        assertTrue(result.data().isEmpty());
    }

    @Test
    void shouldIncludeFromAndToParamsWhenProvided() throws Exception {
        final var from = Instant.parse("2024-01-01T00:00:00Z");
        final var to = Instant.parse("2024-12-31T23:59:59Z");
        final var pageResponse = new SalesApiPageResponse<SalesApiSaleResponse>(List.of(), 0L, 0, 0, 20);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(pageResponse);

        final var result = purchaseService.history("Bearer token", 0, 20, from, to, "req-456").get();

        assertNotNull(result);
        verify(salesApiClient).listSales(anyString(), org.mockito.ArgumentMatchers.argThat(params ->
                params.containsKey("from") && params.containsKey("to")));
    }

    @Test
    void shouldReturnEmptyListWhenPageContentIsNull() throws Exception {
        final var pageResponse = new SalesApiPageResponse<SalesApiSaleResponse>(null, 0L, 0, 0, 20);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(pageResponse);

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-789").get();

        assertNotNull(result);
        assertTrue(result.data().isEmpty());
    }

    private CarApiCarResponse buildAvailableCar(final UUID id) {
        return buildCarWithStatus(id, "AVAILABLE");
    }

    private CarApiCarResponse buildCarWithStatus(final UUID id, final String status) {
        return new CarApiCarResponse(id, "Civic", "Honda", 2023, "White", "Black",
                "VIN123", status, "SEDAN", "CAR", true, BigDecimal.ZERO,
                "GASOLINE", BigDecimal.valueOf(145000), null, List.of(), Instant.now());
    }
}
