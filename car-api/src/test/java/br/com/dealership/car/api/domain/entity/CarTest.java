package br.com.dealership.car.api.domain.entity;

import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarTest {

    private Car buildCar() {
        return Car.builder()
                .id(UUID.randomUUID())
                .model("Civic")
                .manufacturingYear(2022)
                .manufacturer("Honda")
                .externalColor("White")
                .internalColor("Black")
                .vin("1HGBH41JXMN109186")
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.COMBUSTION)
                .listedValue(BigDecimal.valueOf(45000))
                .registrationDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldChangeStatus() {
        var car = buildCar();
        car.changeStatus(CarStatus.SOLD);
        assertEquals(CarStatus.SOLD, car.getStatus());
    }

    @Test
    void shouldChangeListedValue() {
        var car = buildCar();
        car.changeListedValue(BigDecimal.valueOf(55000));
        assertEquals(BigDecimal.valueOf(55000), car.getListedValue());
    }

    @Test
    void shouldChangeImageKey() {
        var car = buildCar();
        car.changeImageKey("cars/new-image.jpg");
        assertEquals("cars/new-image.jpg", car.getImageKey());
    }

    @Test
    void shouldReturnEmptyListWhenOptionalItemsIsNull() {
        var car = Car.builder()
                .model("Civic")
                .manufacturingYear(2022)
                .manufacturer("Honda")
                .externalColor("White")
                .internalColor("Black")
                .vin("1HGBH41JXMN109186")
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.COMBUSTION)
                .listedValue(BigDecimal.valueOf(45000))
                .build();

        assertEquals(List.of(), car.getOptionalItems());
    }

    @Test
    void shouldReturnUnmodifiableListWhenOptionalItemsIsPresent() {
        var car = Car.builder()
                .model("Civic")
                .manufacturingYear(2022)
                .manufacturer("Honda")
                .externalColor("White")
                .internalColor("Black")
                .vin("1HGBH41JXMN109186")
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.COMBUSTION)
                .listedValue(BigDecimal.valueOf(45000))
                .optionalItems(List.of("Sunroof", "GPS"))
                .build();

        assertEquals(2, car.getOptionalItems().size());
    }

    @Test
    void shouldSetRegistrationAndTimestampsOnPrePersist() {
        var car = buildCar().toBuilder()
                .registrationDate(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        car.prePersist();

        assertNotNull(car.getRegistrationDate());
        assertNotNull(car.getCreatedAt());
        assertNotNull(car.getUpdatedAt());
    }

    @Test
    void shouldKeepExistingRegistrationDateOnPrePersist() {
        var existingDate = Instant.parse("2024-01-01T00:00:00Z");
        var car = buildCar().toBuilder()
                .registrationDate(existingDate)
                .createdAt(null)
                .updatedAt(null)
                .build();

        car.prePersist();

        assertEquals(existingDate, car.getRegistrationDate());
        assertNotNull(car.getCreatedAt());
        assertNotNull(car.getUpdatedAt());
    }

    @Test
    void shouldUpdateUpdatedAtOnPreUpdate() {
        var car = buildCar();
        car.preUpdate();
        assertNotNull(car.getUpdatedAt());
    }

    @Test
    void shouldChangeImageKeyToNull() {
        var car = buildCar().toBuilder().imageKey("old-key.jpg").build();
        car.changeImageKey(null);
        assertNull(car.getImageKey());
    }
}

