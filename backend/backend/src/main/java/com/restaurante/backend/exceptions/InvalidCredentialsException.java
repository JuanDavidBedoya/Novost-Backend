package com.restaurante.backend.exceptions;

public class InvalidCredentialsException extends BusinessException {

    //Error de credenciales inexistentes

    public InvalidCredentialsException(String campo, String mensaje) {
        super(campo, mensaje, "INVALID_CREDENTIALS");
    }

    public InvalidCredentialsException(String mensaje) {
        super("general", mensaje, "INVALID_CREDENTIALS");
    }
}
