package br.com.hyteck.school_control.config.jwt.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class JWTProviderTest {

    @InjectMocks
    private JWTProvider jwtProvider;

    @Test
    void testGenerateToken() {
        String token = jwtProvider.generateToken("testuser");
        assertNotNull(token);
    }

    @Test
    void testValidateToken() {
        String token = jwtProvider.generateToken("testuser");
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    void testInvalidToken() {
        String token = "invalidtoken";
        assertTrue(!jwtProvider.validateToken(token));
    }
}
