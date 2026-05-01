package br.com.dealership.dealershibff.feign.car;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class CarApiErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return switch (response.status()) {
            case 404 -> new NotFoundException("Car not found");
            case 503 -> new DownstreamServiceException("Car API is temporarily unavailable");
            default -> {
                if (response.status() >= 400) {
                    yield new DownstreamServiceException(
                            "Car API returned unexpected status: " + response.status());
                }
                yield defaultDecoder.decode(methodKey, response);
            }
        };
    }
}
