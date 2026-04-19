package br.com.dealership.clientapi.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CpfEncryptionConverterTest {

    private CpfEncryptionConverter converter;

    @BeforeEach
    void setUp() {
        String testKey = Base64.getEncoder().encodeToString(new byte[32]);
        converter = new CpfEncryptionConverter(testKey);
    }

    @Test
    void shouldRoundTripEncryptAndDecrypt() {
        final var plaintext = "12345678901";

        final var ciphertext = converter.convertToDatabaseColumn(plaintext);
        final var decrypted = converter.convertToEntityAttribute(ciphertext);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldProduceDifferentCiphertextsForSamePlaintext() {
        final var plaintext = "12345678901";

        final var first = converter.convertToDatabaseColumn(plaintext);
        final var second = converter.convertToDatabaseColumn(plaintext);

        assertNotEquals(first, second);
    }

    @Test
    void shouldThrowOnTamperedCiphertext() {
        assertThrows(IllegalStateException.class,
                () -> converter.convertToEntityAttribute("dGhpcyBpcyBub3QgdmFsaWQgY2lwaGVydGV4dA=="));
    }

    @Test
    void shouldReturnNullWhenEncryptingNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void shouldReturnNullWhenDecryptingNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }
}
