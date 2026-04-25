package br.com.dealership.salesapi.domain.exception;

public final class SaleNotFoundException extends SalesApiException {
    public SaleNotFoundException(String message) {
        super(message);
    }
}
