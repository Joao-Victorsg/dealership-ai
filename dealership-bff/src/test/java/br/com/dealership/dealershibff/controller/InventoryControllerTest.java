package br.com.dealership.dealershibff.controller;

import br.com.dealership.dealershibff.config.GlobalExceptionHandler;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import br.com.dealership.dealershibff.dto.response.ApiResponse;
import br.com.dealership.dealershibff.dto.response.ResponseMeta;
import br.com.dealership.dealershibff.dto.response.VehicleResponse;
import br.com.dealership.dealershibff.service.InventoryService;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200PaginatedListResponse() throws Exception {
        final var vehicle = Instancio.create(VehicleResponse.class);
        final var meta = ResponseMeta.paged("req-id", 0, 20, 1L, 1);
        final var response = ApiResponse.paged(List.of(vehicle), meta);
        when(inventoryService.list(any())).thenReturn(CompletableFuture.completedFuture(response));

        final var mvcResult = mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void shouldReturn200SingleVehicleResponse() throws Exception {
        final var id = UUID.randomUUID();
        final var vehicle = Instancio.create(VehicleResponse.class);
        final var meta = ResponseMeta.of("req-id");
        final var response = ApiResponse.of(vehicle, meta);
        when(inventoryService.getById(id)).thenReturn(CompletableFuture.completedFuture(response));

        final var mvcResult = mockMvc.perform(get("/api/v1/inventory/" + id))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.meta.requestId").value("req-id"));
    }

    @Test
    void shouldReturn404WhenVehicleNotFound() throws Exception {
        final var id = UUID.randomUUID();
        when(inventoryService.getById(id)).thenReturn(
                CompletableFuture.failedFuture(new NotFoundException("Not found")));

        final var mvcResult = mockMvc.perform(get("/api/v1/inventory/" + id))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn503WhenDownstreamUnavailable() throws Exception {
        when(inventoryService.list(any())).thenReturn(
                CompletableFuture.failedFuture(new DownstreamServiceException("unavailable")));

        final var mvcResult = mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("DOWNSTREAM_UNAVAILABLE"));
    }
}
