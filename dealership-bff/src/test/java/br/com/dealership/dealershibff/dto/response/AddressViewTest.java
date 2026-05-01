package br.com.dealership.dealershibff.dto.response;

import br.com.dealership.dealershibff.feign.client.dto.ClientApiAddressResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddressViewTest {

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(AddressView.from(null));
    }

    @Test
    void shouldMapFromSourceWhenNotNull() {
        final var source = new ClientApiAddressResponse(
                "Rua A", "100", null, "Centro", "São Paulo", "SP", "01310100");

        final var result = AddressView.from(source);

        assertNotNull(result);
    }
}
