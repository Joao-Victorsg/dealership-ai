package br.com.dealership.dealershibff.feign.keycloak;

import br.com.dealership.dealershibff.domain.enums.ErrorCode;
import br.com.dealership.dealershibff.domain.exception.BffException;
import br.com.dealership.dealershibff.domain.exception.DownstreamServiceException;
import br.com.dealership.dealershibff.domain.exception.DuplicateIdentityException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class KeycloakErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return switch (response.status()) {
            case 401 -> new BffException(ErrorCode.AUTHENTICATION_REQUIRED, "Invalid credentials");
            case 409 -> new DuplicateIdentityException("User already exists");
            default -> {
                if (response.status() >= 500) {
                    yield new DownstreamServiceException("Keycloak is temporarily unavailable");
                }
                yield defaultDecoder.decode(methodKey, response);
            }
        };
    }
}
