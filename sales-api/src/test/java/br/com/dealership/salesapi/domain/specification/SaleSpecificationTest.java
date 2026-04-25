package br.com.dealership.salesapi.domain.specification;

import br.com.dealership.salesapi.domain.entity.Sale;
import br.com.dealership.salesapi.dto.request.StaffSaleFilterRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleSpecificationTest {

    @Mock
    private Root<Sale> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Predicate predicate;

    @Test
    void fromWithAllNullFiltersProducesNoPredicates() {
        var filter = new StaffSaleFilterRequest(null, null, null, null);
        var spec = SaleSpecification.from(filter);
        spec.toPredicate(root, query, cb);

        verifyNoInteractions(root);
        verifyNoInteractions(cb);
    }

    @Test
    void fromWithClientIdCallsEqual() {
        UUID clientId = UUID.randomUUID();
        var filter = new StaffSaleFilterRequest(clientId, null, null, null);
        when(cb.equal(any(), eq(clientId))).thenReturn(predicate);

        SaleSpecification.from(filter).toPredicate(root, query, cb);

        verify(cb).equal(any(), eq(clientId));
    }

    @Test
    void fromWithCarIdCallsEqual() {
        UUID carId = UUID.randomUUID();
        var filter = new StaffSaleFilterRequest(null, carId, null, null);
        when(cb.equal(any(), eq(carId))).thenReturn(predicate);

        SaleSpecification.from(filter).toPredicate(root, query, cb);

        verify(cb).equal(any(), eq(carId));
    }

    @Test
    void fromWithDateRangeCallsBetween() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-12-31T23:59:59Z");
        var filter = new StaffSaleFilterRequest(null, null, from, to);
        when(cb.between(any(), eq(from), eq(to))).thenReturn(predicate);

        SaleSpecification.from(filter).toPredicate(root, query, cb);

        verify(cb).between(any(), eq(from), eq(to));
    }

    @Test
    void fromWithOnlyFromNullProducesNoBetweenPredicate() {
        Instant to = Instant.now();
        var filter = new StaffSaleFilterRequest(null, null, null, to);

        SaleSpecification.from(filter).toPredicate(root, query, cb);

        verify(cb, never()).between(any(), any(Instant.class), any(Instant.class));
    }

    @Test
    void fromWithOnlyToNullProducesNoBetweenPredicate() {
        Instant from = Instant.now().minusSeconds(3600);
        var filter = new StaffSaleFilterRequest(null, null, from, null);

        SaleSpecification.from(filter).toPredicate(root, query, cb);

        verify(cb, never()).between(any(), any(Instant.class), any(Instant.class));
    }
}
