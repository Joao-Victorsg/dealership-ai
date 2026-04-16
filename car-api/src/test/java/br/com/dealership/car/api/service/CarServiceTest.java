package br.com.dealership.car.api.service;

import br.com.dealership.car.api.dto.request.CarFilterRequest;
import br.com.dealership.car.api.dto.request.CreateCarRequest;
import br.com.dealership.car.api.dto.request.UpdateCarRequest;
import br.com.dealership.car.api.dto.response.PresignedUrlResponse;
import br.com.dealership.car.api.domain.entity.Car;
import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import br.com.dealership.car.api.domain.enums.SortDirection;
import br.com.dealership.car.api.domain.exception.CarNotFoundException;
import br.com.dealership.car.api.domain.exception.DuplicateVinException;
import br.com.dealership.car.api.domain.exception.SoldCarModificationException;
import br.com.dealership.car.api.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CarService carService;

    private static final String VALID_VIN = "ABCDE1234567890AB";

    @BeforeEach
    void setUp() {
        carService = new CarService(carRepository, s3Service, eventPublisher);
    }

    private void stubSave() {
        when(carRepository.save(any(Car.class))).thenAnswer(inv -> {
            var car = inv.getArgument(0, Car.class);
            return car.toBuilder()
                    .id(car.getId() != null ? car.getId() : UUID.randomUUID())
                    .registrationDate(car.getRegistrationDate() != null ? car.getRegistrationDate() : Instant.now())
                    .createdAt(car.getCreatedAt() != null ? car.getCreatedAt() : Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });
    }

    private CreateCarRequest validCreateRequest() {
        return CreateCarRequest.builder()
                .model("Tesla Model 3")
                .manufacturingYear(2020)
                .manufacturer("Tesla")
                .externalColor("White")
                .internalColor("Black")
                .vin(VALID_VIN)
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.ELECTRIC)
                .listedValue(BigDecimal.valueOf(50000))
                .build();
    }

    private Car validCar() {
        return Car.builder()
                .id(UUID.randomUUID())
                .model("Tesla Model 3")
                .manufacturingYear(2020)
                .manufacturer("Tesla")
                .externalColor("White")
                .internalColor("Black")
                .vin(VALID_VIN)
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.ELECTRIC)
                .listedValue(BigDecimal.valueOf(50000))
                .registrationDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private CarFilterRequest emptyFilter() {
        return CarFilterRequest.builder().build();
    }

    @Test
    void shouldRegisterCarWhenAllFieldsValid() {
        stubSave();
        when(carRepository.existsByVin(any())).thenReturn(false);

        var response = carService.registerCar(validCreateRequest());

        assertEquals(VALID_VIN, response.vin());
        verify(carRepository).save(any(Car.class));
    }

    @Test
    void shouldNormalizeVinToUppercase() {
        stubSave();
        var request = CreateCarRequest.builder()
                .model("Tesla Model 3")
                .manufacturingYear(2020)
                .manufacturer("Tesla")
                .externalColor("White")
                .internalColor("Black")
                .vin("abcde1234567890ab")
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.ELECTRIC)
                .listedValue(BigDecimal.valueOf(50000))
                .build();
        when(carRepository.existsByVin(any())).thenReturn(false);

        var response = carService.registerCar(request);

        assertEquals("ABCDE1234567890AB", response.vin());
    }

    @Test
    void shouldTreatEmptyImageKeyAsNull() {
        stubSave();
        when(carRepository.existsByVin(any())).thenReturn(false);
        var request = CreateCarRequest.builder()
                .model("Tesla Model 3")
                .manufacturingYear(2020)
                .manufacturer("Tesla")
                .externalColor("White")
                .internalColor("Black")
                .vin(VALID_VIN)
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.ELECTRIC)
                .listedValue(BigDecimal.valueOf(50000))
                .imageKey("  ")
                .build();

        var response = carService.registerCar(request);

        assertNull(response.imageKey());
    }

    @Test
    void shouldRejectWhenVinAlreadyExists() {
        when(carRepository.existsByVin(VALID_VIN)).thenReturn(true);
        var request = validCreateRequest();

        assertThrows(DuplicateVinException.class, () -> carService.registerCar(request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void shouldReturnCarWhenFoundById() {
        var car = validCar();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));

        var response = carService.getCarById(car.getId());

        assertEquals(car.getId(), response.id());
        assertEquals(car.getVin(), response.vin());
    }

    @Test
    void shouldThrowCarNotFoundExceptionWhenIdDoesNotExist() {
        var id = UUID.randomUUID();
        when(carRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class, () -> carService.getCarById(id));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPaginatedCarList() {
        var car = validCar();
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(car), pageable, 1);
        when(carRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = carService.listCars(emptyFilter(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyPageWhenNoCarsExist() {
        var pageable = PageRequest.of(0, 20);
        var emptyPage = new PageImpl<Car>(List.of(), pageable, 0);
        when(carRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(emptyPage);

        var result = carService.listCars(emptyFilter(), pageable);

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByStatus() {
        var car = validCar();
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(car), pageable, 1);
        when(carRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var filter = CarFilterRequest.builder().status(CarStatus.AVAILABLE).build();
        var result = carService.listCars(filter, pageable);

        assertEquals(1, result.getTotalElements());
        verify(carRepository).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByManufacturer() {
        var car = validCar();
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(car), pageable, 1);
        when(carRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var filter = CarFilterRequest.builder().manufacturer("Toyota").build();
        var result = carService.listCars(filter, pageable);

        assertEquals(1, result.getTotalElements());
        verify(carRepository).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void shouldRejectWhenMinValueExceedsMaxValue() {
        assertThrows(IllegalArgumentException.class,
                () -> CarFilterRequest.builder()
                        .minValue(BigDecimal.valueOf(100000))
                        .maxValue(BigDecimal.valueOf(50000))
                        .build());
    }

    @Test
    void shouldRejectWhenMinYearExceedsMaxYear() {
        assertThrows(IllegalArgumentException.class,
                () -> CarFilterRequest.builder()
                        .minYear(2025)
                        .maxYear(2020)
                        .build());
    }

    @Test
    void shouldUpdateStatusFromAvailableToUnavailable() {
        stubSave();
        var car = validCar();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(CarStatus.UNAVAILABLE, null, null);

        var response = carService.updateCar(car.getId(), request);

        assertEquals(CarStatus.UNAVAILABLE, response.status());
        verify(carRepository).save(any(Car.class));
    }

    @Test
    void shouldRejectUpdateWhenCarIsSold() {
        var car = validCar().toBuilder().status(CarStatus.SOLD).build();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(CarStatus.AVAILABLE, null, null);

        assertThrows(SoldCarModificationException.class, () -> carService.updateCar(car.getId(), request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void shouldUpdateListedValue() {
        stubSave();
        var car = validCar();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(null, BigDecimal.valueOf(75000), null);

        var response = carService.updateCar(car.getId(), request);

        assertEquals(BigDecimal.valueOf(75000), response.listedValue());
        verify(carRepository).save(any(Car.class));
    }

    @Test
    void shouldUpdateImageKey() {
        stubSave();
        var car = validCar().toBuilder().imageKey("cars/old-key.jpg").build();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(null, null, "cars/new-key.jpg");

        var response = carService.updateCar(car.getId(), request);

        assertEquals("cars/new-key.jpg", response.imageKey());
        verify(carRepository).save(any(Car.class));
        verify(eventPublisher).publishEvent(new S3ObjectDeletionEvent("cars/old-key.jpg"));
    }

    @Test
    void shouldEvictCachesOnUpdate() {
        stubSave();
        var car = validCar();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(CarStatus.UNAVAILABLE, null, null);

        carService.updateCar(car.getId(), request);

        verify(carRepository).save(any(Car.class));
    }

    @Test
    void shouldThrowCarNotFoundWhenUpdatingNonexistentCar() {
        var id = UUID.randomUUID();
        when(carRepository.findById(id)).thenReturn(Optional.empty());
        var request = UpdateCarRequest.of(CarStatus.UNAVAILABLE, null, null);

        assertThrows(CarNotFoundException.class, () -> carService.updateCar(id, request));
        verify(carRepository, never()).save(any());
    }

    @Test
    void shouldGeneratePresignedUploadUrlWhenCarExists() {
        var carId = UUID.randomUUID();
        var expectedResponse = PresignedUrlResponse.of("https://s3.amazonaws.com/bucket/cars/image.jpg", "cars/image.jpg", 3600);
        when(carRepository.existsById(carId)).thenReturn(true);
        when(s3Service.generatePresignedPutUrl(carId, "image/jpeg")).thenReturn(expectedResponse);

        var result = carService.generatePresignedUploadUrl(carId, "image/jpeg");

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(s3Service).generatePresignedPutUrl(carId, "image/jpeg");
    }

    @Test
    void shouldThrowCarNotFoundWhenGeneratingPresignedUrlForNonexistentCar() {
        var carId = UUID.randomUUID();
        when(carRepository.existsById(carId)).thenReturn(false);

        assertThrows(CarNotFoundException.class, () -> carService.generatePresignedUploadUrl(carId, "image/jpeg"));
        verify(s3Service, never()).generatePresignedPutUrl(any(), any());
    }

    @Test
    void shouldNotPublishEventWhenUpdatingImageKeyWithNoPreviousKey() {
        stubSave();
        var car = validCar().toBuilder().imageKey(null).build();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(null, null, "cars/new-key.jpg");

        var response = carService.updateCar(car.getId(), request);

        assertEquals("cars/new-key.jpg", response.imageKey());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldClearImageKeyWhenBlankKeyProvided() {
        stubSave();
        var car = validCar().toBuilder().imageKey("cars/old-key.jpg").build();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(null, null, "  ");

        var response = carService.updateCar(car.getId(), request);

        assertNull(response.imageKey());
        verify(eventPublisher).publishEvent(new S3ObjectDeletionEvent("cars/old-key.jpg"));
    }

    @Test
    void shouldNotPublishEventWhenImageKeyIsUnchanged() {
        stubSave();
        var car = validCar().toBuilder().imageKey("cars/same-key.jpg").build();
        when(carRepository.findById(car.getId())).thenReturn(Optional.of(car));
        var request = UpdateCarRequest.of(null, null, "cars/same-key.jpg");

        carService.updateCar(car.getId(), request);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldListCarsWithAscendingSortDirection() {
        var car = validCar();
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(car), pageable, 1);
        when(carRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var filter = CarFilterRequest.builder().sortDirection(SortDirection.ASC).build();
        var result = carService.listCars(filter, pageable);

        assertEquals(1, result.getTotalElements());
    }
}

