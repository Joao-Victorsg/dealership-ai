package br.com.dealership.dealershibff.dto.request;

import br.com.dealership.dealershibff.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank @ValidCpf String cpf,
        @NotBlank @Pattern(regexp = "\\+55\\s?\\(?\\d{2}\\)?\\s?\\d{4,5}[-\\s]?\\d{4}",
                message = "Phone number must be in Brazilian format: +55 (DD) NNNNN-NNNN") String phone,
        @NotBlank String cep,
        @NotBlank String streetNumber
) {
}
