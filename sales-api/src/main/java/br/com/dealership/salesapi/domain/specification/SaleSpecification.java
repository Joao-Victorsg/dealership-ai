package br.com.dealership.salesapi.domain.specification;

import br.com.dealership.salesapi.domain.entity.Sale;
import br.com.dealership.salesapi.domain.entity.Sale_;
import br.com.dealership.salesapi.dto.request.StaffSaleFilterRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class SaleSpecification {

    private SaleSpecification() {}

    public static Specification<Sale> from(StaffSaleFilterRequest filter) {
        return Specification.where(hasClientId(filter.clientId()))
                .and(hasCarId(filter.carId()))
                .and(registeredAtBetween(filter.from(), filter.to()));
    }

    private static Specification<Sale> hasClientId(UUID clientId) {
        return (root, _, cb) -> clientId == null ? null : cb.equal(root.get(Sale_.clientId), clientId);
    }

    private static Specification<Sale> hasCarId(UUID carId) {
        return (root, _, cb) -> carId == null ? null : cb.equal(root.get(Sale_.carId), carId);
    }

    private static Specification<Sale> registeredAtBetween(Instant from, Instant to) {
        return (root, _, cb) -> (from == null || to == null) ? null : cb.between(root.get(Sale_.registeredAt), from, to);
    }
}
