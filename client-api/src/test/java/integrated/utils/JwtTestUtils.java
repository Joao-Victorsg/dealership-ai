package integrated.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JwtTestUtils {

    private static final KeyPair KEY_PAIR;
    private static final RSAKey RSA_KEY;
    private static final JWSSigner SIGNER;
    private static final String KEY_ID = "test-key-id";
    private static String issuer = "https://idp.example.com/realms/dealership";

    static {
        try {
            final var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();

            RSA_KEY = new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic())
                    .keyID(KEY_ID)
                    .build();

            SIGNER = new RSASSASigner(KEY_PAIR.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize JWT test utilities", e);
        }
    }

    public static void setIssuer(String issuerUri) {
        issuer = issuerUri;
    }

    /**
     * Generates a token with a random subject. Suitable for tests that only care about the role.
     */
    public static String generateToken(String... roles) {
        return generateTokenFor(UUID.randomUUID().toString(), roles);
    }

    /**
     * Generates a token with a fixed subject. Use when the test needs to match JWT sub to keycloakId.
     */
    public static String generateTokenFor(String subject, String... roles) {
        try {
            final var now = Instant.now();
            final var claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .claim("realm_access", Map.of("roles", List.of(roles)))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();

            final var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(KEY_ID)
                    .build();

            final var signedJwt = new SignedJWT(header, claims);
            signedJwt.sign(SIGNER);
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public static String getJwksJson() {
        final var publicKey = (RSAPublicKey) KEY_PAIR.getPublic();
        final var encoder = Base64.getUrlEncoder().withoutPadding();

        return """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "kid": "%s",
                      "use": "sig",
                      "alg": "RS256",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """.formatted(
                KEY_ID,
                encoder.encodeToString(publicKey.getModulus().toByteArray()),
                encoder.encodeToString(publicKey.getPublicExponent().toByteArray())
        );
    }

    public static String getWireMockMappingJson() {
        final var jwksBody = getJwksJson().replace("\"", "\\\"").replace("\n", "").replace("  ", "");
        return """
                {
                  "request": {
                    "method": "GET",
                    "url": "/.well-known/jwks.json"
                  },
                  "response": {
                    "status": 200,
                    "headers": {
                      "Content-Type": "application/json"
                    },
                    "body": "%s"
                  }
                }
                """.formatted(jwksBody);
    }
}
