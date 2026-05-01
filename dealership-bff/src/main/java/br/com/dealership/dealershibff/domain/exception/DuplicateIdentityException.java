package br.com.dealership.dealershibff.domain.exception;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

public class DuplicateIdentityException extends BffException {

    public DuplicateIdentityException(final String message) {
        super(ErrorCode.DUPLICATE_IDENTITY, message);
    }

    public DuplicateIdentityException(final String message, final Throwable cause) {
        super(ErrorCode.DUPLICATE_IDENTITY, message, cause);
    }
}
