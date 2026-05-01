package br.com.dealership.dealershibff.dto.response;

public record ClientSnapshot(
        String firstName,
        String lastName,
        String cpf
) {

    public static ClientSnapshot of(final String firstName, final String lastName, final String cpf) {
        return new ClientSnapshot(firstName, lastName, cpf);
    }
}
