package br.com.dealership.salesapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ClientSnapshotRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @NotBlank @Email String email,
        @Valid @NotNull AddressSnapshotRequest address
) {
}
