package br.com.dealership.car.api.domain.specification;

import br.com.dealership.car.api.domain.entity.Car;
import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import br.com.dealership.car.api.dto.request.CarFilterRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class CarSpecificationTest {

    @Mock private Root<Car> root;
    @Mock private CriteriaQuery query;
    @Mock private CriteriaBuilder cb;
    @Mock private Predicate predicate;
    @Mock private Expression lowerExpr;
    @Mock private Path path;

    @Test
    void shouldReturnNullWhenAllFiltersAreNull() {
        var filter = CarFilterRequest.builder().build();
        assertNull(CarSpecification.from(filter).toPredicate(root, query, cb));
    }

    @Test
    void shouldReturnNullPredicateForBlankManufacturer() {
        var filter = CarFilterRequest.builder().manufacturer("   ").build();
        assertNull(CarSpecification.from(filter).toPredicate(root, query, cb));
    }

    @Test
    void shouldApplyStatusFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).equal(path, CarStatus.AVAILABLE);

        var filter = CarFilterRequest.builder().status(CarStatus.AVAILABLE).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(path, CarStatus.AVAILABLE);
    }

    @Test
    void shouldApplyCategoryFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).equal(path, CarCategory.SEDAN);

        var filter = CarFilterRequest.builder().category(CarCategory.SEDAN).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(path, CarCategory.SEDAN);
    }

    @Test
    void shouldApplyPropulsionTypeFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).equal(path, PropulsionType.ELECTRIC);

        var filter = CarFilterRequest.builder().propulsionType(PropulsionType.ELECTRIC).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(path, PropulsionType.ELECTRIC);
    }

    @Test
    void shouldApplyManufacturerFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(lowerExpr).when(cb).lower(path);
        doReturn(predicate).when(cb).like(lowerExpr, "%tesla%");

        var filter = CarFilterRequest.builder().manufacturer("Tesla").build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).like(any(Expression.class), eq("%tesla%"));
    }

    @Test
    void shouldApplyIsNewFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).equal(path, true);

        var filter = CarFilterRequest.builder().isNew(true).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(path, true);
    }

    @Test
    void shouldApplyOnlyMaxValueFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).lessThanOrEqualTo(path, BigDecimal.valueOf(50000));

        var filter = CarFilterRequest.builder().maxValue(BigDecimal.valueOf(50000)).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void shouldApplyOnlyMinValueFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).greaterThanOrEqualTo(path, BigDecimal.valueOf(20000));

        var filter = CarFilterRequest.builder().minValue(BigDecimal.valueOf(20000)).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void shouldApplyBothValueFilters() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).between(path, BigDecimal.valueOf(20000), BigDecimal.valueOf(50000));

        var filter = CarFilterRequest.builder()
                .minValue(BigDecimal.valueOf(20000))
                .maxValue(BigDecimal.valueOf(50000))
                .build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).between(any(Expression.class), any(Comparable.class), any(Comparable.class));
    }

    @Test
    void shouldApplyOnlyMaxYearFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).lessThanOrEqualTo(path, 2024);

        var filter = CarFilterRequest.builder().maxYear(2024).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void shouldApplyOnlyMinYearFilter() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).greaterThanOrEqualTo(path, 2020);

        var filter = CarFilterRequest.builder().minYear(2020).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void shouldApplyBothYearFilters() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).between(path, 2020, 2024);

        var filter = CarFilterRequest.builder().minYear(2020).maxYear(2024).build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).between(any(Expression.class), any(Comparable.class), any(Comparable.class));
    }

    @Test
    void shouldApplyMultipleFiltersSimultaneously() {
        doReturn(path).when(root).get(nullable(SingularAttribute.class));
        doReturn(predicate).when(cb).equal(path, CarStatus.AVAILABLE);
        doReturn(predicate).when(cb).equal(path, CarCategory.SEDAN);
        doReturn(lowerExpr).when(cb).lower(path);
        doReturn(predicate).when(cb).like(lowerExpr, "%honda%");
        doReturn(predicate).when(cb).equal(path, false);
        doReturn(predicate).when(cb).between(path, BigDecimal.valueOf(30000), BigDecimal.valueOf(70000));
        doReturn(predicate).when(cb).between(path, 2020, 2024);
        doReturn(predicate).when(cb).and(predicate, predicate);

        var filter = CarFilterRequest.builder()
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .manufacturer("Honda")
                .isNew(false)
                .minValue(BigDecimal.valueOf(30000))
                .maxValue(BigDecimal.valueOf(70000))
                .minYear(2020)
                .maxYear(2024)
                .build();
        var result = CarSpecification.from(filter).toPredicate(root, query, cb);

        assertNotNull(result);
    }
}
