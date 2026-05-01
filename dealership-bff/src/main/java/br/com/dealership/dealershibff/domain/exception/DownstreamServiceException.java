package br.com.dealership.dealershibff.domain.exception;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;

public class DownstreamServiceException extends BffException {

    public DownstreamServiceException(final String message) {
        super(ErrorCode.DOWNSTREAM_UNAVAILABLE, message);
    }

    public DownstreamServiceException(final String message, final Throwable cause) {
        super(ErrorCode.DOWNSTREAM_UNAVAILABLE, message, cause);
    }
}
