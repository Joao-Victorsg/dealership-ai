package br.com.dealership.clientapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCpfRequest(
        @NotBlank @ValidCpf String cpf
) {
}
