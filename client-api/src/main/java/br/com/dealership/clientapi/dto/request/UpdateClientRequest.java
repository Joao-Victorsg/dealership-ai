package br.com.dealership.clientapi.dto.request;

@ValidAddressFields
public record UpdateClientRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String postcode,
        String streetNumber
) {

    public UpdateClientRequest {
        if (firstName == null && lastName == null && phoneNumber == null
                && postcode == null && streetNumber == null) {
            throw new IllegalArgumentException("No fields to update");
        }
    }
}
