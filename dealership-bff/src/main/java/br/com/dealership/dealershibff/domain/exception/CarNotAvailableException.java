package br.com.dealership.dealershibff.domain.exception;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

public class CarNotAvailableException extends BffException {

    public CarNotAvailableException(final String message) {
        super(ErrorCode.CAR_NOT_AVAILABLE, message);
    }

    public CarNotAvailableException(final String message, final Throwable cause) {
        super(ErrorCode.CAR_NOT_AVAILABLE, message, cause);
    }
}
