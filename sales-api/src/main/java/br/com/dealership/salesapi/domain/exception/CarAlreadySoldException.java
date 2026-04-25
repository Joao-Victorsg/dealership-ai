package br.com.dealership.salesapi.domain.exception;

public final class CarAlreadySoldException extends SalesApiException {
    public CarAlreadySoldException(String message) {
        super(message);
    }
}
