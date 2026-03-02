package com.restaurante.backend.exceptions;

/**
 * Excepción para errores de autenticación (credenciales incorrectas, código inválido, etc.)
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException(String campo, String mensaje) {
        super(campo, mensaje, "INVALID_CREDENTIALS");
    }

    public InvalidCredentialsException(String mensaje) {
        super("general", mensaje, "INVALID_CREDENTIALS");
    }
}
