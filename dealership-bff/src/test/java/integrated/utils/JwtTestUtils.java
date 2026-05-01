package integrated.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class JwtTestUtils {

    private static final RSAKey RSA_KEY;
    public static final String KEY_ID = "test-key-id";

    static {
        try {
            RSA_KEY = new RSAKeyGenerator(2048)
                    .keyID(KEY_ID)
                    .generate();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JwtTestUtils() {
    }

    public static RSAKey getPublicKey() {
        return RSA_KEY.toPublicJWK();
    }

    public static String generateToken(final String subject, final List<String> roles, final String email) {
        try {
            final var claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("http://localhost/realms/dealership")
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                    .issueTime(new Date())
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", email)
                    .claim("roles", roles);

            if (roles != null && !roles.isEmpty()) {
                claimsBuilder.claim("realm_access", java.util.Map.of("roles", roles));
            }

            final var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(KEY_ID)
                    .build();

            final var jwt = new SignedJWT(header, claimsBuilder.build());
            jwt.sign(new RSASSASigner(RSA_KEY));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test JWT", e);
        }
    }

    public static String generateClientToken(final String subject) {
        return generateToken(subject, List.of("CLIENT"), subject + "@test.com");
    }

    public static String generateExpiredToken(final String subject) {
        try {
            final var claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("http://localhost/realms/dealership")
                    .expirationTime(new Date(System.currentTimeMillis() - 3_600_000))
                    .issueTime(new Date(System.currentTimeMillis() - 7_200_000))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", subject + "@test.com")
                    .claim("roles", List.of("CLIENT"))
                    .build();

            final var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(KEY_ID)
                    .build();

            final var jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(RSA_KEY));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate expired test JWT", e);
        }
    }
}
