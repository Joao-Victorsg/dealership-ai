package br.com.dealership.salesapi.service;

import br.com.dealership.salesapi.domain.entity.CarStatus;
import br.com.dealership.salesapi.domain.entity.Sale;
import br.com.dealership.salesapi.domain.exception.CarAlreadySoldException;
import br.com.dealership.salesapi.domain.exception.CarNotAvailableException;
import br.com.dealership.salesapi.domain.exception.SaleOwnershipException;
import br.com.dealership.salesapi.dto.request.RegisterSaleRequest;
import br.com.dealership.salesapi.dto.request.StaffSaleFilterRequest;
import br.com.dealership.salesapi.dto.response.SaleResponse;
import br.com.dealership.salesapi.messaging.SaleEventPayload;
import br.com.dealership.salesapi.messaging.SnsPublisher;
import br.com.dealership.salesapi.repository.SaleRepository;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static br.com.dealership.salesapi.domain.specification.SaleSpecification.from;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SnsPublisher snsPublisher;
    private final SaleCacheService saleCacheService;

    public SaleService(SaleRepository saleRepository, SnsPublisher snsPublisher,
                       @Lazy SaleCacheService saleCacheService) {
        this.saleRepository = saleRepository;
        this.snsPublisher = snsPublisher;
        this.saleCacheService = saleCacheService;
    }

    @Transactional
    public SaleResponse registerSale(RegisterSaleRequest request, JwtAuthenticationToken token) {
        final var clientId = UUID.fromString(token.getName());

        if (!clientId.equals(request.clientId())) {
            throw new SaleOwnershipException("Token subject does not match clientId in request");
        }

        if (request.carSnapshot().status() != CarStatus.AVAILABLE) {
            throw new CarNotAvailableException(
                    "Car is not available for sale: " + request.carSnapshot().status());
        }

        final var saleValue = request.carSnapshot().listedValue()
                .multiply(BigDecimal.valueOf(1.10))
                .setScale(4, RoundingMode.HALF_UP);

        final Sale sale;
        try {
            sale = saleRepository.save(Sale.from(request, clientId, saleValue));
        } catch (DataIntegrityViolationException _) {
            throw new CarAlreadySoldException("Car already sold: " + request.carId());
        }

        MDC.put("sale.id", sale.getId().toString());
        MDC.put("sale.clientId", sale.getClientId().toString());
        MDC.put("sale.carId", sale.getCarId().toString());
        MDC.put("sale.value", sale.getSaleValue().toPlainString());

        snsPublisher.publish(SaleEventPayload.from(sale));

        return SaleResponse.from(sale);
    }

    public Page<SaleResponse> getClientSales(UUID clientId, Instant from, Instant to,
                                              Pageable pageable) {
        if (from != null && to != null) {
            return saleRepository.findByClientIdAndRegisteredAtBetween(clientId, from, to, pageable)
                    .map(SaleResponse::from);
        }
        return saleRepository.findByClientId(clientId, pageable).map(SaleResponse::from);
    }

    public SaleResponse getById(UUID id, JwtAuthenticationToken token, boolean isStaff) {
        final var saleResponse = saleCacheService.findSaleById(id);
        if (!isStaff) {
            final var requestingClientId = UUID.fromString(token.getName());
            if (!saleResponse.clientId().equals(requestingClientId)) {
                throw new SaleOwnershipException("Access denied: sale belongs to another client");
            }
        }
        return saleResponse;
    }

    public Page<SaleResponse> getStaffSales(StaffSaleFilterRequest filter, Pageable pageable) {
        return saleRepository.findAll(from(filter), pageable).map(SaleResponse::from);
    }
}
