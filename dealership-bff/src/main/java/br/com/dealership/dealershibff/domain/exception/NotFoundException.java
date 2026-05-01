package br.com.dealership.dealershibff.domain.exception;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

public class NotFoundException extends BffException {

    public NotFoundException(final String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
