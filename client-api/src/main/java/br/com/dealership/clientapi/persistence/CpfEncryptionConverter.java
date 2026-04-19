package br.com.dealership.clientapi.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
public class CpfEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public CpfEncryptionConverter(@Value("${app.cpf.encryption-key}") String base64Key) {
        final byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            final byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            final byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            final byte[] ivAndCiphertext = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, ivAndCiphertext, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, ivAndCiphertext, GCM_IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(ivAndCiphertext);
        } catch (Exception e) {
            throw new IllegalStateException("CPF encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            final byte[] ivAndCiphertext = Base64.getDecoder().decode(ciphertext);

            final byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ivAndCiphertext, 0, iv, 0, GCM_IV_LENGTH);

            final byte[] encrypted = new byte[ivAndCiphertext.length - GCM_IV_LENGTH];
            System.arraycopy(ivAndCiphertext, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new IllegalStateException("CPF decryption failed", e);
        }
    }
}
