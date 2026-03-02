package com.restaurante.backend.exceptions;

public class TokenExpiredException extends BusinessException {

    public TokenExpiredException(String mensaje) {
        super("general", mensaje, "TOKEN_EXPIRED");
    }
}
