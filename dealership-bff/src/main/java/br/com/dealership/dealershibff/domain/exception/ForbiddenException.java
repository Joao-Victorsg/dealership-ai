package br.com.dealership.dealershibff.domain.exception;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

public class ForbiddenException extends BffException {

    public ForbiddenException(final String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
