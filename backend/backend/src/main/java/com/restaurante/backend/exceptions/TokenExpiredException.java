package com.restaurante.backend.exceptions;

public class TokenExpiredException extends BusinessException {

    //Error de token de validación expirado

    public TokenExpiredException(String mensaje) {
        super("general", mensaje, "TOKEN_EXPIRED");
    }
}
