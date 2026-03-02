package com.restaurante.backend.exceptions;

/**
 * Excepción para errores de pago
 */
public class PaymentException extends BusinessException {

    public PaymentException(String mensaje) {
        super("general", mensaje, "PAYMENT_ERROR");
    }

    public PaymentException(String campo, String mensaje) {
        super(campo, mensaje, "PAYMENT_ERROR");
    }
}
