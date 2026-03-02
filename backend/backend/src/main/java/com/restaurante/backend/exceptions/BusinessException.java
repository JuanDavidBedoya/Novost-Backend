package com.restaurante.backend.exceptions;

public class BusinessException extends RuntimeException {
    
    private final String campo;
    private final String codigoError;

    public BusinessException(String mensaje) {
        super(mensaje);
        this.campo = "general";
        this.codigoError = "BUSINESS_ERROR";
    }

    public BusinessException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
        this.codigoError = "BUSINESS_ERROR";
    }

    public BusinessException(String campo, String mensaje, String codigoError) {
        super(mensaje);
        this.campo = campo;
        this.codigoError = codigoError;
    }

    public String getCampo() {
        return campo;
    }

    public String getCodigoError() {
        return codigoError;
    }
}
