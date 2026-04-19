package br.com.dealership.clientapi.client.dto;

public record ViaCepResponse(
        String cep,
        String logradouro,
        String localidade,
        String uf,
        Boolean erro
) {
}
