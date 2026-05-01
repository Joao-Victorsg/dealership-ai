package br.com.dealership.dealershibff.feign.client;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.DuplicateIdentityException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class ClientApiErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return switch (response.status()) {
            case 404 -> new NotFoundException("Client not found");
            case 409 -> new DuplicateIdentityException("Client already exists");
            default -> {
                if (response.status() >= 500) {
                    yield new DownstreamServiceException("Client API is temporarily unavailable");
                }
                yield defaultDecoder.decode(methodKey, response);
            }
        };
    }
}
