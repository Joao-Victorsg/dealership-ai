package br.com.dealership.car.api.repository;

import java.util.UUID;

import br.com.dealership.car.api.domain.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CarRepository extends JpaRepository<Car, UUID>, JpaSpecificationExecutor<Car> {

    boolean existsByVin(String vin);
}

