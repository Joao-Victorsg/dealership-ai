package br.com.dealership.salesapi.controller;

import br.com.dealership.salesapi.config.GlobalExceptionHandler;
import br.com.dealership.salesapi.domain.entity.CarStatus;
import br.com.dealership.salesapi.domain.exception.CarAlreadySoldException;
import br.com.dealership.salesapi.domain.exception.CarNotAvailableException;
import br.com.dealership.salesapi.domain.exception.SaleNotFoundException;
import br.com.dealership.salesapi.domain.exception.SaleOwnershipException;
import br.com.dealership.salesapi.domain.exception.SnsPublishException;
import br.com.dealership.salesapi.dto.request.AddressSnapshotRequest;
import br.com.dealership.salesapi.dto.request.CarSnapshotRequest;
import br.com.dealership.salesapi.dto.request.ClientSnapshotRequest;
import br.com.dealership.salesapi.dto.request.RegisterSaleRequest;
import br.com.dealership.salesapi.dto.request.StaffSaleFilterRequest;
import br.com.dealership.salesapi.dto.response.SaleResponse;
import br.com.dealership.salesapi.service.SaleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

    @Mock
    private SaleService saleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SaleController(saleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void registerSaleReturns201WithLocationHeaderOnSuccess() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        SaleResponse saleResponse = Instancio.of(SaleResponse.class)
                .create();
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any())).thenReturn(saleResponse);

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void registerSaleReturns400WhenBodyIsInvalid() throws Exception {
        var token = buildToken(UUID.randomUUID(), "CLIENT");
        String invalidBody = "{}";

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerSaleReturns403OnSaleOwnershipException() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any()))
                .thenThrow(new SaleOwnershipException("forbidden"));

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerSaleReturns422OnCarNotAvailableException() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any()))
                .thenThrow(new CarNotAvailableException("car not available"));

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void registerSaleReturns422OnCarAlreadySoldException() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any()))
                .thenThrow(new CarAlreadySoldException("already sold"));

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void registerSaleReturns503OnSnsPublishException() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any()))
                .thenThrow(new SnsPublishException("sns unavailable"));

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getClientSalesReturns200WithPaginatedResults() throws Exception {
        UUID clientId = UUID.randomUUID();
        var token = buildToken(clientId, "CLIENT");
        var saleResponse = Instancio.create(SaleResponse.class);
        when(saleService.getClientSales(eq(clientId), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(saleResponse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/sales")
                        .principal(token))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getClientSalesByIdReturns200ForOwnSale() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        var token = buildToken(clientId, "CLIENT");
        SaleResponse saleResponse = Instancio.create(SaleResponse.class);
        when(saleService.getById(eq(saleId), any(), eq(false))).thenReturn(saleResponse);

        mockMvc.perform(get("/api/v1/sales/{id}", saleId)
                        .principal(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getClientSalesByIdReturns403OnOwnershipException() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        var token = buildToken(clientId, "CLIENT");
        when(saleService.getById(eq(saleId), any(), eq(false)))
                .thenThrow(new SaleOwnershipException("forbidden"));

        mockMvc.perform(get("/api/v1/sales/{id}", saleId)
                        .principal(token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClientSalesByIdReturns404WhenNotFound() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        var token = buildToken(clientId, "CLIENT");
        when(saleService.getById(eq(saleId), any(), eq(false)))
                .thenThrow(new SaleNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/sales/{id}", saleId)
                        .principal(token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStaffSalesReturns200ForStaffRole() throws Exception {
        var token = buildToken(UUID.randomUUID(), "STAFF");
        when(saleService.getStaffSales(any(StaffSaleFilterRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/sales/staff")
                        .principal(token))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdReturns200ForStaffRole() throws Exception {
        UUID saleId = UUID.randomUUID();
        var token = buildToken(UUID.randomUUID(), "STAFF");
        SaleResponse saleResponse = Instancio.create(SaleResponse.class);
        when(saleService.getById(eq(saleId), any(), eq(true))).thenReturn(saleResponse);

        mockMvc.perform(get("/api/v1/sales/{id}", saleId)
                        .principal(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getByIdReturns200ForAdminRole() throws Exception {
        UUID saleId = UUID.randomUUID();
        var token = buildToken(UUID.randomUUID(), "ADMIN");
        SaleResponse saleResponse = Instancio.create(SaleResponse.class);
        when(saleService.getById(eq(saleId), any(), eq(true))).thenReturn(saleResponse);

        mockMvc.perform(get("/api/v1/sales/{id}", saleId)
                        .principal(token))
                .andExpect(status().isOk());
    }

    @Test
    void registerSaleReturns400OnIllegalArgumentException() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any()))
                .thenThrow(new IllegalArgumentException("invalid argument"));

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerSaleReturns500OnUnexpectedException() throws Exception {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId);
        var token = buildToken(clientId, "CLIENT");
        when(saleService.registerSale(any(), any()))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(post("/api/v1/sales")
                        .principal(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    private JwtAuthenticationToken buildToken(UUID subject, String role) {
        var token = mock(JwtAuthenticationToken.class);
        when(token.getName()).thenReturn(subject.toString());
        Collection<org.springframework.security.core.GrantedAuthority> authorities =
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + role));
        lenient().when(token.getAuthorities()).thenReturn(authorities);
        return token;
    }

    private RegisterSaleRequest buildValidRequest(UUID clientId) {
        var addr = new AddressSnapshotRequest("St", "1", null, "Nb", "City", "SP", "00000-000");
        var client = new ClientSnapshotRequest("John", "Doe", "12345678901", "j@e.com", addr);
        var car = new CarSnapshotRequest("Model", "Brand", "Red", "Black", 2020,
                List.of(), "Sedan", "Premium", "ABC12345678901234",
                BigDecimal.valueOf(50000), CarStatus.AVAILABLE);
        return new RegisterSaleRequest(UUID.randomUUID(), clientId, client, car);
    }
}
