package br.com.dealership.clientapi.dto.response;

import br.com.dealership.clientapi.entity.Client;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Residential address")
public record AddressResponse(
        @Schema(description = "Brazilian postcode (CEP)") String postcode,
        @Schema(description = "Street or building number") String streetNumber,
        @Schema(description = "Street name resolved from ViaCEP") String streetName,
        @Schema(description = "City") String city,
        @Schema(description = "State abbreviation (UF)") String state,
        @Schema(description = "Whether the address was resolved via ViaCEP") boolean addressSearched
) {

    public static AddressResponse from(Client.Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getPostcode(),
                address.getStreetNumber(),
                address.getStreetName(),
                address.getCity(),
                address.getState(),
                address.isAddressSearched()
        );
    }
}
