package com.restaurante.backend.exceptions;

public class BusinessException extends RuntimeException {

    // Excepción personalizada para errores de lógica de negocio con información de campo y código
    
    private final String campo;
    private final String codigoError;

    // Constructor simple: mensaje genérico con campo "general" y código por defecto

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

    // Constructor completo: permite especificar campo, mensaje y código de error personalizado

    public BusinessException(String campo, String mensaje, String codigoError) {
        super(mensaje);
        this.campo = campo;
        this.codigoError = codigoError;
    }

    // Getters: accesores para obtener campo y código de error

    public String getCampo() {
        return campo;
    }

    public String getCodigoError() {
        return codigoError;
    }
}
