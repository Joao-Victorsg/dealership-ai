package br.com.dealership.salesapi.service;

import br.com.dealership.salesapi.domain.entity.CarStatus;
import br.com.dealership.salesapi.domain.entity.Sale;
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
import br.com.dealership.salesapi.messaging.SaleEventPayload;
import br.com.dealership.salesapi.messaging.SnsPublisher;
import br.com.dealership.salesapi.repository.SaleRepository;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SnsPublisher snsPublisher;

    @Mock
    private SaleCacheService saleCacheService;

    @InjectMocks
    private SaleService saleService;


    @Test
    void registerSaleComputesSaleValueWithTenPercentTax() {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId, CarStatus.AVAILABLE, BigDecimal.valueOf(10000));
        var token = mockToken(clientId);

        Sale savedSale = buildSaleEntity(clientId, request, BigDecimal.valueOf(11000.0000));
        when(saleRepository.save(any())).thenReturn(savedSale);

        SaleResponse result = saleService.registerSale(request, token);

        assertEquals(BigDecimal.valueOf(11000.0000).setScale(4), result.saleValue());
        assertNotNull(result.id());
        verify(snsPublisher).publish(any(SaleEventPayload.class));
    }

    @Test
    void registerSaleThrowsCarNotAvailableWhenStatusIsSold() {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId, CarStatus.SOLD, BigDecimal.valueOf(10000));
        var token = mockToken(clientId);

        assertThrows(CarNotAvailableException.class,
                () -> saleService.registerSale(request, token));
        verifyNoInteractions(saleRepository);
    }

    @Test
    void registerSaleThrowsCarNotAvailableWhenStatusIsUnavailable() {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId, CarStatus.UNAVAILABLE, BigDecimal.valueOf(10000));
        var token = mockToken(clientId);

        assertThrows(CarNotAvailableException.class,
                () -> saleService.registerSale(request, token));
    }

    @Test
    void registerSaleThrowsSaleOwnershipWhenClientIdMismatch() {
        UUID tokenSubject = UUID.randomUUID();
        UUID differentClientId = UUID.randomUUID();
        var request = buildValidRequest(differentClientId, CarStatus.AVAILABLE, BigDecimal.valueOf(10000));
        var token = mockToken(tokenSubject);

        assertThrows(SaleOwnershipException.class,
                () -> saleService.registerSale(request, token));
        verifyNoInteractions(saleRepository);
    }

    @Test
    void registerSaleThrowsCarAlreadySoldOnDataIntegrityViolation() {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId, CarStatus.AVAILABLE, BigDecimal.valueOf(10000));
        var token = mockToken(clientId);

        when(saleRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThrows(CarAlreadySoldException.class,
                () -> saleService.registerSale(request, token));
    }

    @Test
    void registerSalePropagatesSnsPublishException() {
        UUID clientId = UUID.randomUUID();
        var request = buildValidRequest(clientId, CarStatus.AVAILABLE, BigDecimal.valueOf(10000));
        var token = mockToken(clientId);
        Sale savedSale = buildSaleEntity(clientId, request, BigDecimal.valueOf(11000.0000));
        when(saleRepository.save(any())).thenReturn(savedSale);
        doThrow(SnsPublishException.class).when(snsPublisher).publish(any());

        assertThrows(SnsPublishException.class,
                () -> saleService.registerSale(request, token));
    }

    @Test
    void getClientSalesReturnsPaginatedResults() {
        UUID clientId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findByClientId(clientId, pageable))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getClientSales(clientId, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getClientSalesWithDateRangeUsesCorrectMethod() {
        UUID clientId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T23:59:59Z");
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findByClientIdAndRegisteredAtBetween(clientId, from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getClientSales(clientId, from, to, pageable);

        assertEquals(1, result.getTotalElements());
        verify(saleRepository, never()).findByClientId(any(), any());
    }

    @Test
    void getClientSalesReturnsEmptyPageWhenNoResults() {
        UUID clientId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(saleRepository.findByClientId(clientId, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        var result = saleService.getClientSales(clientId, null, null, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void getByIdReturnsSaleResponseForOwner() {
        UUID clientId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        SaleResponse saleResponse = Instancio.of(SaleResponse.class)
                .set(Select.field(SaleResponse::clientId), clientId)
                .create();
        var token = mockToken(clientId);
        when(saleCacheService.findSaleById(saleId)).thenReturn(saleResponse);

        SaleResponse result = saleService.getById(saleId, token, false);

        assertEquals(saleResponse, result);
    }

    @Test
    void getByIdThrowsSaleOwnershipForDifferentClient() {
        UUID ownerClientId = UUID.randomUUID();
        UUID requestingClientId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        SaleResponse saleResponse = Instancio.of(SaleResponse.class)
                .set(Select.field(SaleResponse::clientId), ownerClientId)
                .create();
        var token = mockToken(requestingClientId);
        when(saleCacheService.findSaleById(saleId)).thenReturn(saleResponse);

        assertThrows(SaleOwnershipException.class,
                () -> saleService.getById(saleId, token, false));
    }

    @Test
    void getByIdAllowsStaffToAccessAnySale() {
        UUID ownerClientId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        SaleResponse saleResponse = Instancio.of(SaleResponse.class)
                .set(Select.field(SaleResponse::clientId), ownerClientId)
                .create();
        var token = mockToken(staffId);
        when(saleCacheService.findSaleById(saleId)).thenReturn(saleResponse);

        SaleResponse result = saleService.getById(saleId, token, true);

        assertEquals(saleResponse, result);
    }

    @Test
    void getByIdThrowsSaleNotFoundWhenMissing() {
        UUID saleId = UUID.randomUUID();
        var token = mockToken(UUID.randomUUID());
        when(saleCacheService.findSaleById(saleId))
                .thenThrow(new SaleNotFoundException("not found"));

        assertThrows(SaleNotFoundException.class,
                () -> saleService.getById(saleId, token, false));
    }

    @Test
    void getStaffSalesWithNoFiltersReturnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getStaffSales(new StaffSaleFilterRequest(null, null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffSalesWithClientIdFilterReturnsSales() {
        UUID clientId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getStaffSales(new StaffSaleFilterRequest(clientId, null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffSalesWithCarIdFilterReturnsSales() {
        UUID carId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getStaffSales(new StaffSaleFilterRequest(null, carId, null, null), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffSalesWithDateRangeFilterReturnsSales() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T23:59:59Z");
        Pageable pageable = PageRequest.of(0, 20);
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getStaffSales(new StaffSaleFilterRequest(null, null, from, to), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffSalesWithAllFiltersReturnsIntersection() {
        UUID clientId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T23:59:59Z");
        Pageable pageable = PageRequest.of(0, 20);
        var sale = Instancio.create(Sale.class);
        when(saleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sale)));

        var result = saleService.getStaffSales(new StaffSaleFilterRequest(clientId, carId, from, to), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStaffSalesReturnsEmptyPageWhenNoResults() {
        Pageable pageable = PageRequest.of(0, 20);
        when(saleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = saleService.getStaffSales(new StaffSaleFilterRequest(null, null, null, null), pageable);

        assertTrue(result.isEmpty());
    }

    private JwtAuthenticationToken mockToken(UUID subject) {
        var token = mock(JwtAuthenticationToken.class);
        lenient().when(token.getName()).thenReturn(subject.toString());
        return token;
    }

    private RegisterSaleRequest buildValidRequest(UUID clientId, CarStatus status,
                                                   BigDecimal listedValue) {
        var addr = new AddressSnapshotRequest("St", "1", null, "Nb", "City", "SP", "00000-000");
        var client = new ClientSnapshotRequest("John", "Doe", "12345678901", "j@e.com", addr);
        var car = new CarSnapshotRequest("Model", "Brand", "Red", "Black", 2020,
                List.of(), "Sedan", "Premium", "ABC12345678901234", listedValue, status);
        return new RegisterSaleRequest(UUID.randomUUID(), clientId, client, car);
    }

    private Sale buildSaleEntity(UUID clientId, RegisterSaleRequest request,
                                  BigDecimal saleValue) {
        return Instancio.of(Sale.class)
                .set(Select.field(Sale::getClientId), clientId)
                .set(Select.field(Sale::getSaleValue), saleValue.setScale(4))
                .create();
    }
}
