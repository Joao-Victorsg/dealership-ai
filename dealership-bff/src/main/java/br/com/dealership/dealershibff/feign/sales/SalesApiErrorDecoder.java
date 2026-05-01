package br.com.dealership.dealershibff.feign.sales;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;
import br.com.dealership.dealershibff.domain.exception.CarNotAvailableException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class SalesApiErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return switch (response.status()) {
            case 409 -> new CarNotAvailableException("Car is no longer available");
            default -> new DownstreamServiceException("Sales API error: " + response.status());
        };
    }
}
