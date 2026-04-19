package br.com.dealership.clientapi.exception;

public final class DuplicateKeycloakIdException extends ClientApiException {
    public DuplicateKeycloakIdException(String message) {
        super(message);
    }
}
