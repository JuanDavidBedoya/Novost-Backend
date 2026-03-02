package com.restaurante.backend.exceptions;

/**
 * Excepción para tokens o códigos expirados
 */
public class TokenExpiredException extends BusinessException {

    public TokenExpiredException(String mensaje) {
        super("general", mensaje, "TOKEN_EXPIRED");
    }
}
