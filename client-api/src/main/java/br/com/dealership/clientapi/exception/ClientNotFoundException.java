package br.com.dealership.clientapi.exception;

public final class ClientNotFoundException extends ClientApiException {
    public ClientNotFoundException(String message) {
        super(message);
    }
}
