package br.com.dealership.car.api.domain.exception;

public final class SoldCarModificationException extends CarApiException {

    public SoldCarModificationException() {
        super("Cannot modify a sold car");
    }
}

