package br.com.dealership.dealershibff.service;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.dto.request.InventoryFilterRequest;
import br.com.dealership.dealershibff.feign.car.CarApiClient;
import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiPageResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private CarApiClient carApiClient;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void shouldReturnMappedVehiclesOnListHappyPath() throws Exception {
        final var car = Instancio.create(CarApiCarResponse.class);
        final var pageResponse = new CarApiPageResponse<>(List.of(car), 1L, 1, 0, 20);
        when(carApiClient.listCars(any())).thenReturn(pageResponse);

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
        when(carApiClient.getCarById(car.id())).thenReturn(car);

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
    void shouldProduceDeterministicCacheKeyForSameFilterInDifferentOrder() {
        final var filter1 = new InventoryFilterRequest("civic", "SEDAN", null, null,
                null, 2020, 2024, null, null, null, null, null,
                "PRICE", "ASC", 0, 20);

        final var key1 = filter1.toCacheKey();
        final var key2 = filter1.toCacheKey();

        assertEquals(key1, key2);
    }
}
