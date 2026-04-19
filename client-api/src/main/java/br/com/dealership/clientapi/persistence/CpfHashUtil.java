package br.com.dealership.clientapi.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class CpfHashUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secretBytes;

    public CpfHashUtil(@Value("${app.cpf.hmac-secret}") String hmacSecret) {
        this.secretBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String cpf) {
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, ALGORITHM));
            final byte[] digest = mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("CPF hash computation failed", e);
        }
    }
}
