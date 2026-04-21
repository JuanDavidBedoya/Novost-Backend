package com.restaurante.backend.exceptions;

public class ValidationException extends BusinessException {

    //Error en la validación de los campos

    public ValidationException(String campo, String mensaje) {
        super(campo, mensaje, "VALIDATION_ERROR");
    }

    public ValidationException(String mensaje) {
        super("general", mensaje, "VALIDATION_ERROR");
    }
}
