package br.com.dealership.clientapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateClientRequest(
        @NotBlank String keycloakId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @ValidCpf String cpf,
        @NotBlank @Pattern(regexp = "\\+55\\s?\\(?\\d{2}\\)?\\s?\\d{4,5}[-\\s]?\\d{4}",
                message = "Phone number must be in Brazilian format: +55 (DD) NNNNN-NNNN") String phoneNumber,
        @NotBlank String postcode,
        @NotBlank String streetNumber
) {

    public CreateClientRequest {
        keycloakId = trim(keycloakId);
        firstName = trim(firstName);
        lastName = trim(lastName);
        cpf = trim(cpf);
        phoneNumber = trim(phoneNumber);
        postcode = trim(postcode);
        streetNumber = trim(streetNumber);
    }

    private static String trim(String value) {
        return value != null ? value.strip() : null;
    }
}
