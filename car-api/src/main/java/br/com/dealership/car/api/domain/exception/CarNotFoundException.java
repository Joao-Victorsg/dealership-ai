package br.com.dealership.car.api.domain.exception;

import java.util.UUID;

public final class CarNotFoundException extends CarApiException {

    public CarNotFoundException(UUID id) {
        super("Car not found with id: " + id);
    }
}

