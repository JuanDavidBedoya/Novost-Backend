package com.restaurante.backend.services;

import com.restaurante.backend.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        when(jwtProperties.secret()).thenReturn("mySecretKeyForJWTTokenGenerationThatIsLongEnough");

        userDetails = new User("juan@example.com", "password", 
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("USUARIO")));
    }

    @Test
    void generateToken_GeneraTokenExitosamente() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ExtraeUsernameDelToken() {
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertEquals("juan@example.com", username);
    }

    @Test
    void isTokenValid_TokenValido_ReturnsTrue() {
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_TokenConUsuarioIncorrecto_ReturnsFalse() {
        String token = jwtService.generateToken(userDetails);
        
        UserDetails otroUsuario = new User("otro@example.com", "password", 
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("USUARIO")));

        boolean isValid = jwtService.isTokenValid(token, otroUsuario);

        assertFalse(isValid);
    }

    @Test
    void generateToken_TieneExpirationCorrecta() {
        String token = jwtService.generateToken(userDetails);

        java.util.Date expiration = jwtService.extractClaim(token, 
            io.jsonwebtoken.Claims::getExpiration);

        java.util.Date now = new java.util.Date();
        long diffInMillies = Math.abs(expiration.getTime() - now.getTime());
        long diffInMinutes = diffInMillies / (60 * 1000);

        assertTrue(diffInMinutes >= 9 && diffInMinutes <= 11);
    }
}
