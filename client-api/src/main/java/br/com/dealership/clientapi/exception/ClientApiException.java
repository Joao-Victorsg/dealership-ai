package br.com.dealership.clientapi.exception;

public sealed class ClientApiException extends RuntimeException
        permits ClientNotFoundException, DuplicateCpfException, DuplicateKeycloakIdException, ProfileInactiveException {

    protected ClientApiException(String message) {
        super(message);
    }
}
