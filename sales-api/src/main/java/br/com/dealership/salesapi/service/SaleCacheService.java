package br.com.dealership.salesapi.service;

import br.com.dealership.salesapi.domain.exception.SaleNotFoundException;
import br.com.dealership.salesapi.dto.response.SaleResponse;
import br.com.dealership.salesapi.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleCacheService {

    private final SaleRepository saleRepository;

    @Cacheable(cacheNames = "sales", key = "#id")
    public SaleResponse findSaleById(UUID id) {
        return saleRepository.findById(id)
                .map(SaleResponse::from)
                .orElseThrow(() -> new SaleNotFoundException("Sale not found: " + id));
    }
}
