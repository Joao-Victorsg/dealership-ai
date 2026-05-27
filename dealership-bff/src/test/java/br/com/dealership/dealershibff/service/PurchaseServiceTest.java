package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.feign.car.CarApiClient;
import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiDataResponse;
import br.com.dealership.dealershibff.feign.client.ClientApiClient;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiAddressResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiDataResponse;
import br.com.dealership.dealershibff.feign.sales.SalesApiClient;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiDataResponse;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiPageResponse;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiSaleResponse;
import org.instancio.Instancio;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        final var clientId = UUID.randomUUID();
        final var car = buildAvailableCar(carId);
        final var client = buildClient();
        final var saleResponse = Instancio.create(SalesApiSaleResponse.class);

        when(carApiClient.getCarById(carId)).thenReturn(new CarApiDataResponse<>(car));
        when(clientApiClient.getMe(anyString())).thenReturn(new ClientApiDataResponse<>(client));
        when(salesApiClient.registerSale(anyString(), any())).thenReturn(new SalesApiDataResponse<>(saleResponse));

        final var result = purchaseService.purchase(carId, "Bearer token", "user@test.com", clientId).get();

        assertNotNull(result);
        assertEquals(saleResponse.id(), result.id());
        verify(salesApiClient, times(1)).registerSale(eq("Bearer token"),
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.clientId().equals(clientId)
                                && request.clientSnapshot() != null
                                && request.clientSnapshot().address() != null
                                && request.carSnapshot() != null
                                && "AVAILABLE".equals(request.carSnapshot().status())));
    }

    @Test
    void shouldThrowCarNotAvailableExceptionWhenCarStatusIsNotAvailable() {
        final var carId = UUID.randomUUID();
        final var car = buildCarWithStatus(carId, "SOLD");
        when(carApiClient.getCarById(carId)).thenReturn(new CarApiDataResponse<>(car));

        final var future = purchaseService.purchase(carId, "Bearer token", "user@test.com", UUID.randomUUID());
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(CarNotAvailableException.class, ex.getCause().getClass());
        verify(salesApiClient, never()).registerSale(anyString(), any());
    }

    @Test
    void shouldThrowCarNotAvailableWhenSalesApiReturns409AndCallExactlyOnce() {
        final var carId = UUID.randomUUID();
        final var clientId = UUID.randomUUID();
        final var car = buildAvailableCar(carId);
        final var client = buildClient();

        when(carApiClient.getCarById(carId)).thenReturn(new CarApiDataResponse<>(car));
        when(clientApiClient.getMe(anyString())).thenReturn(new ClientApiDataResponse<>(client));
        when(salesApiClient.registerSale(anyString(), any())).thenThrow(new CarNotAvailableException("Already sold"));

        final var future = purchaseService.purchase(carId, "Bearer token", "user@test.com", clientId);
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(CarNotAvailableException.class, ex.getCause().getClass());
        verify(salesApiClient, times(1)).registerSale(anyString(), any());
    }

    @Test
    void shouldThrowDownstreamServiceExceptionWhenSalesApiReturns5xx() {
        final var carId = UUID.randomUUID();
        final var clientId = UUID.randomUUID();
        final var car = buildAvailableCar(carId);
        final var client = buildClient();

        when(carApiClient.getCarById(carId)).thenReturn(new CarApiDataResponse<>(car));
        when(clientApiClient.getMe(anyString())).thenReturn(new ClientApiDataResponse<>(client));
        when(salesApiClient.registerSale(anyString(), any())).thenThrow(new DownstreamServiceException("unavailable"));

        final var future = purchaseService.purchase(carId, "Bearer token", "user@test.com", clientId);
        final var ex = assertThrows(ExecutionException.class, future::get);

        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
        verify(salesApiClient, times(1)).registerSale(anyString(), any());
    }

    @Test
    void shouldReturnPaginatedHistoryWithCorrectMeta() throws Exception {
        final var sale = Instancio.create(SalesApiSaleResponse.class);
        final var pageMeta = new SalesApiPageResponse.PageMetadata(20, 0, 1L, 1);
        final var pageResponse = new SalesApiPageResponse<>(List.of(sale), pageMeta);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(new SalesApiDataResponse<>(pageResponse));

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-123").get();

        assertNotNull(result);
        assertEquals(1, result.data().size());
        assertEquals(1L, result.meta().totalElements());
    }

    @Test
    void shouldParseSpringPageStyleHistoryPayload() throws Exception {
        final var sale = Instancio.create(SalesApiSaleResponse.class);
        final var pageResponse = new SalesApiPageResponse<>(
                List.of(sale),
                null,
                20,
                0,
                1L,
                1
        );
        when(salesApiClient.listSales(anyString(), any())).thenReturn(new SalesApiDataResponse<>(pageResponse));

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-spring-page").get();

        assertNotNull(result);
        assertEquals(1, result.data().size());
        assertEquals(0, result.meta().page());
        assertEquals(20, result.meta().pageSize());
        assertEquals(1L, result.meta().totalElements());
        assertEquals(1, result.meta().totalPages());
    }

    @Test
    void shouldReturnEmptyListWhenNoHistory() throws Exception {
        final var pageMeta = new SalesApiPageResponse.PageMetadata(20, 0, 0L, 0);
        final var pageResponse = new SalesApiPageResponse<SalesApiSaleResponse>(List.of(), pageMeta);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(new SalesApiDataResponse<>(pageResponse));

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-123").get();

        assertNotNull(result);
        assertTrue(result.data().isEmpty());
    }

    @Test
    void shouldIncludeFromAndToParamsWhenProvided() throws Exception {
        final var from = Instant.parse("2024-01-01T00:00:00Z");
        final var to = Instant.parse("2024-12-31T23:59:59Z");
        final var pageMeta = new SalesApiPageResponse.PageMetadata(20, 0, 0L, 0);
        final var pageResponse = new SalesApiPageResponse<SalesApiSaleResponse>(List.of(), pageMeta);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(new SalesApiDataResponse<>(pageResponse));

        final var result = purchaseService.history("Bearer token", 0, 20, from, to, "req-456").get();

        assertNotNull(result);
        verify(salesApiClient).listSales(anyString(), org.mockito.ArgumentMatchers.argThat(params ->
                params.containsKey("from") && params.containsKey("to")));
    }

    @Test
    void shouldReturnEmptyListWhenPageContentIsNull() throws Exception {
        final var pageMeta = new SalesApiPageResponse.PageMetadata(20, 0, 0L, 0);
        final var pageResponse = new SalesApiPageResponse<SalesApiSaleResponse>(null, pageMeta);
        when(salesApiClient.listSales(anyString(), any())).thenReturn(new SalesApiDataResponse<>(pageResponse));

        final var result = purchaseService.history("Bearer token", 0, 20, null, null, "req-789").get();

        assertNotNull(result);
        assertTrue(result.data().isEmpty());
    }

    private CarApiCarResponse buildAvailableCar(final UUID id) {
        return buildCarWithStatus(id, "AVAILABLE");
    }

    private CarApiCarResponse buildCarWithStatus(final UUID id, final String status) {
        return new CarApiCarResponse(id, "Civic", "Honda", 2023, "White", "Black",
                "1HGBH41JXMN109186", status, "SEDAN", "CAR", true, BigDecimal.ZERO,
                "GASOLINE", BigDecimal.valueOf(145000), null, List.of(), Instant.now());
    }

    private ClientApiClientResponse buildClient() {
        return new ClientApiClientResponse(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "Joao",
                "Silva",
                "52998224725",
                "11987654321",
                java.time.LocalDateTime.now(),
                null,
                new ClientApiAddressResponse("01001000", "100", "Rua A", "Sao Paulo", "SP", true)
        );
    }
}
