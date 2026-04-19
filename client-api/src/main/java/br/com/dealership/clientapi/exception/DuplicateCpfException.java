package br.com.dealership.clientapi.exception;

public final class DuplicateCpfException extends ClientApiException {

    private final String cpf;

    public DuplicateCpfException(String message, String cpf) {
        super(message);
        this.cpf = cpf;
    }

    public DuplicateCpfException(String message) {
        this(message, null);
    }

    public String getCpf() {
        return cpf;
    }
}
