package br.com.dealership.salesapi.domain.exception;

public sealed class SalesApiException extends RuntimeException
        permits CarNotAvailableException, SaleOwnershipException, CarAlreadySoldException,
                SaleNotFoundException, SnsPublishException {

    protected SalesApiException(String message) {
        super(message);
    }
}
