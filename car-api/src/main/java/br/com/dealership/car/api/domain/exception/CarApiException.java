package br.com.dealership.car.api.domain.exception;

public sealed class CarApiException extends RuntimeException
        permits CarNotFoundException, DuplicateVinException, SoldCarModificationException {

    protected CarApiException(String message) {
        super(message);
    }
}
