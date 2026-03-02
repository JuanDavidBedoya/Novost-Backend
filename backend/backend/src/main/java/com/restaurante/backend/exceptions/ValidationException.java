package com.restaurante.backend.exceptions;

/**
 * Excepción para errores de validación de negocio
 */
public class ValidationException extends BusinessException {

    public ValidationException(String campo, String mensaje) {
        super(campo, mensaje, "VALIDATION_ERROR");
    }

    public ValidationException(String mensaje) {
        super("general", mensaje, "VALIDATION_ERROR");
    }
}
