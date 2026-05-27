package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.dto.request.InventoryFilterRequest;
import br.com.dealership.dealershibff.feign.car.CarApiClient;
import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiDataResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiFilterParams;
import br.com.dealership.dealershibff.feign.car.dto.CarApiFilterOptionsResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiPageResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private CarApiClient carApiClient;

    private InventoryService inventoryService;

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(carApiClient, DIRECT_EXECUTOR);
    }

    @Test
    void shouldReturnMappedVehiclesOnListHappyPath() throws Exception {
        final var car = Instancio.create(CarApiCarResponse.class);
        final var pageMeta = new CarApiPageResponse.PageMetadata(20, 0, 1L, 1);
        final var pageResponse = new CarApiPageResponse<>(List.of(car), pageMeta);
        when(carApiClient.listCars(any())).thenReturn(new CarApiDataResponse<>(pageResponse));

        final var filter = new InventoryFilterRequest(null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, 0, 20);

        final var result = inventoryService.list(filter).get();

        assertNotNull(result);
        assertEquals(1, result.data().size());
        assertEquals(car.id(), result.data().getFirst().id());
        assertEquals(1L, result.meta().totalElements());
    }

    @Test
    void shouldReturnMappedVehicleOnGetByIdHappyPath() throws Exception {
        final var car = Instancio.create(CarApiCarResponse.class);
        when(carApiClient.getCarById(car.id())).thenReturn(new CarApiDataResponse<>(car));

        final var result = inventoryService.getById(car.id()).get();

        assertNotNull(result);
        assertEquals(car.id(), result.data().id());
        assertEquals(car.model(), result.data().model());
    }

    @Test
    void shouldPropagateDownstreamServiceExceptionOnList() {
        when(carApiClient.listCars(any())).thenThrow(new DownstreamServiceException("unavailable"));

        final var filter = new InventoryFilterRequest(null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, 0, 20);

        final var future = inventoryService.list(filter);
        final var ex = assertThrows(ExecutionException.class, future::get);
        assertNotNull(ex.getCause());
        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
    }

    @Test
    void shouldPropagateDownstreamServiceExceptionOnGetById() {
        final var id = UUID.randomUUID();
        when(carApiClient.getCarById(id)).thenThrow(new DownstreamServiceException("unavailable"));

        final var future = inventoryService.getById(id);
        final var ex = assertThrows(ExecutionException.class, future::get);
        assertNotNull(ex.getCause());
        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
    }

    @Test
    void shouldReturnFilterOptionsOnHappyPath() throws Exception {
        when(carApiClient.getFilterOptions()).thenReturn(new CarApiDataResponse<>(
                new CarApiFilterOptionsResponse(
                        List.of("Honda", "Toyota"),
                        List.of("Black", "White")
                )
        ));

        final var result = inventoryService.filterOptions().get();

        assertNotNull(result);
        assertEquals(List.of("Honda", "Toyota"), result.data().manufacturers());
        assertEquals(List.of("Black", "White"), result.data().exteriorColors());
    }

    @Test
    void shouldPropagateDownstreamServiceExceptionOnFilterOptions() {
        when(carApiClient.getFilterOptions()).thenThrow(new DownstreamServiceException("unavailable"));

        final var future = inventoryService.filterOptions();
        final var ex = assertThrows(ExecutionException.class, future::get);
        assertNotNull(ex.getCause());
        assertEquals(DownstreamServiceException.class, ex.getCause().getClass());
    }

    @Test
    void shouldProduceDeterministicCacheKeyForSameFilterInDifferentOrder() {
        final var filter1 = new InventoryFilterRequest("civic", "SEDAN", null, null,
                null, 2020, 2024, null, null, null, null, null,
                "PRICE", "ASC", 0, 20);

        final var key1 = filter1.toCacheKey();
        final var key2 = filter1.toCacheKey();

        assertEquals(key1, key2);
    }

    @Test
    void shouldNormalizeSortParamsBeforeCallingCarApi() throws Exception {
        final var pageMeta = new CarApiPageResponse.PageMetadata(20, 0, 0L, 0);
        final var pageResponse = new CarApiPageResponse<CarApiCarResponse>(List.of(), pageMeta);
        when(carApiClient.listCars(any())).thenReturn(new CarApiDataResponse<>(pageResponse));

        final var filter = new InventoryFilterRequest(null, null, null, null, null,
                null, null, null, null, null, null, null,
                "registrationDate", "desc", 0, 20);

        inventoryService.list(filter).get();

        final var captor = ArgumentCaptor.forClass(CarApiFilterParams.class);
        verify(carApiClient).listCars(captor.capture());
        final var params = captor.getValue();

        assertEquals("AVAILABLE", params.status());
        assertEquals("REGISTRATION_DATE", params.sortBy());
        assertEquals("DESC", params.sortDirection());
    }

    @Test
    void shouldMapFrontendFiltersToCarApiCompatibleFields() throws Exception {
        final var pageMeta = new CarApiPageResponse.PageMetadata(20, 0, 0L, 0);
        final var pageResponse = new CarApiPageResponse<CarApiCarResponse>(List.of(), pageMeta);
        when(carApiClient.listCars(any())).thenReturn(new CarApiDataResponse<>(pageResponse));

        final var filter = new InventoryFilterRequest(
                "civic",
                "HATCHBACK",
                "GASOLINE",
                "USED",
                "Honda",
                2020,
                2025,
                java.math.BigDecimal.valueOf(70000),
                java.math.BigDecimal.valueOf(150000),
                "Pearl White",
                null,
                null,
                "PRICE",
                "ASC",
                0,
                20
        );

        inventoryService.list(filter).get();

        final var captor = ArgumentCaptor.forClass(CarApiFilterParams.class);
        verify(carApiClient).listCars(captor.capture());
        final var params = captor.getValue();

        assertEquals("AVAILABLE", params.status());
        assertEquals("HATCH", params.category());
        assertEquals("COMBUSTION", params.propulsionType());
        assertEquals(false, params.isNew());
        assertEquals(2020, params.minYear());
        assertEquals(2025, params.maxYear());
        assertEquals("Pearl White", params.externalColor());
        assertEquals("LISTED_VALUE", params.sortBy());
    }

    @Test
    void shouldAlwaysEnforceAvailableStatusWhenListingCars() throws Exception {
        final var pageMeta = new CarApiPageResponse.PageMetadata(20, 0, 0L, 0);
        final var pageResponse = new CarApiPageResponse<CarApiCarResponse>(List.of(), pageMeta);
        when(carApiClient.listCars(any())).thenReturn(new CarApiDataResponse<>(pageResponse));

        final var filter = new InventoryFilterRequest(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, 0, 20
        );

        inventoryService.list(filter).get();

        final var captor = ArgumentCaptor.forClass(CarApiFilterParams.class);
        verify(carApiClient).listCars(captor.capture());
        assertEquals("AVAILABLE", captor.getValue().status());
    }
}
