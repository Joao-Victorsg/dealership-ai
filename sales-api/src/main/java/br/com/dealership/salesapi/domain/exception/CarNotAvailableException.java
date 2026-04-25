package br.com.dealership.salesapi.domain.exception;

public final class CarNotAvailableException extends SalesApiException {
    public CarNotAvailableException(String message) {
        super(message);
    }
}
