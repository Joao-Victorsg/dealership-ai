package br.com.dealership.salesapi.repository;

import br.com.dealership.salesapi.domain.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    Page<Sale> findByClientId(UUID clientId, Pageable pageable);

    Page<Sale> findByClientIdAndRegisteredAtBetween(UUID clientId, Instant from, Instant to, Pageable pageable);
}
