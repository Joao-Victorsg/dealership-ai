package br.com.dealership.dealershibff.feign.client;

import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.DuplicateIdentityException;
import br.com.dealership.dealershibff.domain.exception.ForbiddenException;
import br.com.dealership.dealershibff.domain.exception.NotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class ClientApiErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return switch (response.status()) {
            case 403 -> new ForbiddenException("Access denied");
            case 404 -> new NotFoundException("Client not found");
            case 409 -> new DuplicateIdentityException("Client already exists");
            case 422 -> new DuplicateIdentityException("A profile already exists for this Keycloak account");
            default -> {
                if (response.status() >= 500) {
                    yield new DownstreamServiceException("Client API is temporarily unavailable");
                }
                yield defaultDecoder.decode(methodKey, response);
            }
        };
    }
}
