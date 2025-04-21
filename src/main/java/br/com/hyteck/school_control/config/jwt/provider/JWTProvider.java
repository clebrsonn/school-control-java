package br.com.hyteck.school_control.config.jwt.provider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

import static org.springframework.security.config.Elements.JWT;

@Component
public class JWTProvider {
    private final byte[] jwtSecret;

    private final long jwtExpirationInMs;

    public JWTProvider(@Value("${jwt.secret}") final String secret, @Value("${jwt.expires}") final String expiration) {
        this.jwtSecret = secret.getBytes();
        this.jwtExpirationInMs = Long.parseLong(expiration);
    }

    public String generateToken(String username) {
        return Jwts.builder().subject(username)
                .issuedAt(Date.from(Instant.now()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret))
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret))
                .build().parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(Keys.hmacShaKeyFor(jwtSecret)).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
