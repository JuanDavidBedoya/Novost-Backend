package com.restaurante.backend.services;

import com.restaurante.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function; 

// Servicio para generación, validación y extracción de claims de tokens JWT

@Service
public class JwtService {

    // Inyección de dependencia: propiedades de configuración JWT (clave secreta, duración)

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // Método extractUsername: extrae el nombre de usuario (subject) del token

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Método extractClaim: extrae un claim específico aplicando función resolver

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Método generateToken: crea token JWT firmado con expiración de 1 hora

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSignInKey())
                .compact();
    }

    // Método isTokenValid: valida que el token pertenece al usuario y no ha expirado

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Método isTokenExpired: verifica si la fecha de expiración ya pasó

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Método extractExpiration: extrae fecha de expiración del token

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Método extractAllClaims: parsea y obtiene todos los claims del token JWT firmado

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
    }

    // Método getSignInKey: genera clave HMAC-SHA a partir de la clave secreta configurada

    private SecretKey getSignInKey() {
        byte[] keyBytes = jwtProperties.secret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}