package br.com.hyteck.school_control.config.jwt.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Component responsible for JWT (JSON Web Token) generation, parsing, and validation.
 * It uses a secret key and expiration time configured in application properties.
 */
@Component
public class JWTProvider {
    private static final Logger logger = LoggerFactory.getLogger(JWTProvider.class);
    private final byte[] jwtSecret; // Secret key for signing and verifying tokens, stored as bytes.
    private final long jwtExpirationInMs; // Token expiration time in milliseconds.

    /**
     * Constructs a JWTProvider with the secret key and expiration time.
     * These values are typically injected from application properties.
     *
     * @param secret     The JWT secret key as a String.
     * @param expiration The JWT expiration time in milliseconds as a String.
     */
    public JWTProvider(@Value("${jwt.secret}") final String secret, @Value("${jwt.expires}") final String expiration) {
        this.jwtSecret = secret.getBytes(); // Convert secret string to byte array for HMAC algorithms.
        this.jwtExpirationInMs = Long.parseLong(expiration); // Parse expiration string to long.
    }

    /**
     * Generates a JWT token for the given username.
     * The token includes the username as the subject, an issuer, an issued-at date,
     * an expiration date, and is signed with the configured secret key.
     *
     * @param username The username for whom the token is generated.
     * @return A signed JWT token string.
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expirationDate = Date.from(now.plusMillis(jwtExpirationInMs));

        return Jwts.builder()
                .subject(username) // Set the subject of the token (typically the user identifier).
                .issuer("school-control") // Set the issuer of the token.
                .issuedAt(issuedAt) // Set the time the token was issued.
                .expiration(expirationDate) // Set the expiration time of the token.
                .signWith(Keys.hmacShaKeyFor(jwtSecret)) // Sign the token with the HMAC SHA key.
                .compact(); // Build the token and serialize it to a compact, URL-safe string.
    }

    /**
     * Extracts the username (subject) from a given JWT token.
     * It verifies the token's signature using the configured secret key.
     *
     * @param token The JWT token string.
     * @return The username (subject) extracted from the token.
     * @throws JwtException If the token is invalid or cannot be parsed.
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret)) // Set the key to verify the signature.
                .build()
                .parseSignedClaims(token) // Parse the token string.
                .getPayload() // Get the claims (payload) part of the token.
                .getSubject(); // Get the subject claim (username).
    }

    /**
     * Validates a given JWT token.
     * Checks include:
     * - Signature verification using the secret key.
     * - Expiration time (token must not be expired).
     * - Issuer (token must be issued by "school-control").
     *
     * @param token The JWT token string to validate.
     * @return {@code true} if the token is valid, {@code false} otherwise.
     */
    public boolean validateToken(String token) {
        try {
            // Parse the token and verify its signature.
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret)) // Provide the key for signature verification.
                .build()
                .parseSignedClaims(token); // This will throw JwtException if signature is invalid or token is malformed.

            // Check if the token is expired.
            if (claims.getPayload().getExpiration().before(new Date())) {
                logger.warn("Token has expired: {}", token);
                return false;
            }
            logger.info("validando token");

            return "school-control".equals(claims.getPayload().getIssuer());
        } catch (JwtException | IllegalArgumentException ex) {
            // Log common JWT validation errors.
            logger.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }
}
