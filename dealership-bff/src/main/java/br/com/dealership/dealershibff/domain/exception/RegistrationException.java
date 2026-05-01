package br.com.dealership.dealershibff.domain.exception;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

public class RegistrationException extends BffException {

    public RegistrationException(final String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public RegistrationException(final String message, final Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
