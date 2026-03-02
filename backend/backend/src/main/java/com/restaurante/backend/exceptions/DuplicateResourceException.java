package com.restaurante.backend.exceptions;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String campo, String valor) {
        super(campo, valor + " ya está en uso", "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String mensaje) {
        super("general", mensaje, "DUPLICATE_RESOURCE");
    }
}
