package com.restaurante.backend.exceptions;

public class ResourceNotFoundException extends BusinessException {

    //Error de recurso no encontrado

    public ResourceNotFoundException(String recurso, String identificador) {
        super("general", recurso + " no encontrado con identificador: " + identificador, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String mensaje) {
        super("general", mensaje, "RESOURCE_NOT_FOUND");
    }
}
