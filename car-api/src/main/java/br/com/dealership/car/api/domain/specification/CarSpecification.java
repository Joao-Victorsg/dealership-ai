package br.com.dealership.car.api.domain.specification;

import br.com.dealership.car.api.dto.request.CarFilterRequest;
import br.com.dealership.car.api.domain.entity.Car;
import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.entity.Car_;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class CarSpecification {

    private CarSpecification() {
    }

    public static Specification<Car> from(CarFilterRequest filter) {
        return Specification
                .where(hasStatus(filter.status()))
                .and(hasCategory(filter.category()))
                .and(hasPropulsionType(filter.propulsionType()))
                .and(hasManufacturer(filter.manufacturer()))
                .and(isNew(filter.isNew()))
                .and(valueBetween(filter.minValue(), filter.maxValue()))
                .and(yearBetween(filter.minYear(), filter.maxYear()));
    }

    private static Specification<Car> hasStatus(CarStatus status) {
        return (root, _, cb) -> status == null ? null
                : cb.equal(root.get(Car_.status), status);
    }

    private static Specification<Car> hasCategory(CarCategory category) {
        return (root, _, cb) -> category == null ? null
                : cb.equal(root.get(Car_.category), category);
    }

    private static Specification<Car> hasPropulsionType(PropulsionType propulsionType) {
        return (root, _, cb) -> propulsionType == null ? null
                : cb.equal(root.get(Car_.propulsionType), propulsionType);
    }

    private static Specification<Car> hasManufacturer(String manufacturer) {
        return (root, _, cb) -> manufacturer == null || manufacturer.isBlank() ? null
                : cb.like(cb.lower(root.get(Car_.manufacturer)),
                          "%" + manufacturer.toLowerCase() + "%");
    }

    private static Specification<Car> isNew(Boolean isNew) {
        return (root, _, cb) -> isNew == null ? null
                : cb.equal(root.get(Car_.isNew), isNew);
    }

    private static Specification<Car> valueBetween(BigDecimal min, BigDecimal max) {
        return (root, _, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get(Car_.listedValue), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get(Car_.listedValue), min);
            return cb.between(root.get(Car_.listedValue), min, max);
        };
    }

    private static Specification<Car> yearBetween(Integer min, Integer max) {
        return (root, _, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get(Car_.manufacturingYear), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get(Car_.manufacturingYear), min);
            return cb.between(root.get(Car_.manufacturingYear), min, max);
        };
    }
}
