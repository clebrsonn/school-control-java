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

@Component
public class JWTProvider {
    private static final Logger logger = LoggerFactory.getLogger(JWTProvider.class);
    private final byte[] jwtSecret;
    private final long jwtExpirationInMs;

    public JWTProvider(@Value("${jwt.secret}") final String secret, @Value("${jwt.expires}") final String expiration) {
        this.jwtSecret = secret.getBytes();
        this.jwtExpirationInMs = Long.parseLong(expiration);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuer("school-control")
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
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret))
                .build()
                .parseSignedClaims(token);

            if (claims.getPayload().getExpiration().before(new Date())) {
                return false;
            }
            
            if (!"school-control".equals(claims.getPayload().getIssuer())) {
                return false;
            }
            
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.error("Token inválido: " + ex.getMessage());
            return false;
        }
    }
}
