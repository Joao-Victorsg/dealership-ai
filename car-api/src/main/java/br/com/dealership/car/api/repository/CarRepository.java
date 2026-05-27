package br.com.dealership.car.api.repository;

import java.util.UUID;
import java.util.List;

import br.com.dealership.car.api.domain.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CarRepository extends JpaRepository<Car, UUID>, JpaSpecificationExecutor<Car> {

    boolean existsByVin(String vin);

    @Query("""
            select distinct c.manufacturer
            from Car c
            where c.manufacturer is not null
              and trim(c.manufacturer) <> ''
            """)
    List<String> findDistinctManufacturers();

    @Query("""
            select distinct c.externalColor
            from Car c
            where c.externalColor is not null
              and trim(c.externalColor) <> ''
            """)
    List<String> findDistinctExteriorColors();
}

