package br.com.dealership.car.api.service;

import br.com.dealership.car.api.dto.request.CarFilterRequest;
import br.com.dealership.car.api.dto.response.CarResponse;
import br.com.dealership.car.api.dto.request.CreateCarRequest;
import br.com.dealership.car.api.dto.response.PresignedUrlResponse;
import br.com.dealership.car.api.dto.request.UpdateCarRequest;
import br.com.dealership.car.api.domain.entity.Car;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.exception.CarNotFoundException;
import br.com.dealership.car.api.domain.exception.DuplicateVinException;
import br.com.dealership.car.api.domain.exception.SoldCarModificationException;
import br.com.dealership.car.api.repository.CarRepository;
import br.com.dealership.car.api.domain.specification.CarSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    public CarService(CarRepository carRepository, S3Service s3Service, ApplicationEventPublisher eventPublisher) {
        this.carRepository = carRepository;
        this.s3Service = s3Service;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @CacheEvict(value = "car-listings", allEntries = true)
    public CarResponse registerCar(CreateCarRequest request) {
        if (carRepository.existsByVin(request.vin())) {
            throw new DuplicateVinException(request.vin());
        }

        var car = Car.builder()
                .model(request.model())
                .manufacturingYear(request.manufacturingYear())
                .manufacturer(request.manufacturer())
                .externalColor(request.externalColor())
                .internalColor(request.internalColor())
                .vin(request.vin())
                .status(request.status())
                .optionalItems(request.optionalItems())
                .category(request.category())
                .kilometers(request.kilometers())
                .isNew(request.isNew())
                .propulsionType(request.propulsionType())
                .listedValue(request.listedValue())
                .imageKey(request.imageKey())
                .build();

        var saved = carRepository.save(car);
        return CarResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "car-by-id", key = "#id")
    public CarResponse getCarById(UUID id) {
        var car = carRepository.findById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        return CarResponse.from(car);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "car-listings",
            key = "#filter.toString() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize"
    )
    public Page<CarResponse> listCars(CarFilterRequest filter, Pageable pageable) {
        var sort = Sort.by(filter.sortDirection().toSpringDirection(), filter.sortBy().fieldName());
        var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        var spec = CarSpecification.from(filter);
        return carRepository.findAll(spec, pageRequest).map(CarResponse::from);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "car-by-id", key = "#id"),
            @CacheEvict(value = "car-listings", allEntries = true)
    })
    public CarResponse updateCar(UUID id, UpdateCarRequest request) {
        var car = carRepository.findById(id)
                .orElseThrow(() -> new CarNotFoundException(id));

        if (CarStatus.SOLD == car.getStatus()) {
            throw new SoldCarModificationException();
        }

        if (request.status() != null) {
            car.changeStatus(request.status());
        }

        if (request.listedValue() != null) {
            car.changeListedValue(request.listedValue());
        }

        if (request.imageKey() != null) {
            var previousKey = car.getImageKey();
            var newKey = request.imageKey().isBlank() ? null : request.imageKey();
            car.changeImageKey(newKey);

            if (previousKey != null && !previousKey.equals(newKey)) {
                eventPublisher.publishEvent(new S3ObjectDeletionEvent(previousKey));
            }
        }

        var saved = carRepository.save(car);
        return CarResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse generatePresignedUploadUrl(UUID carId, String contentType) {
        if (!carRepository.existsById(carId)) {
            throw new CarNotFoundException(carId);
        }
        return s3Service.generatePresignedPutUrl(carId, contentType);
    }
}
