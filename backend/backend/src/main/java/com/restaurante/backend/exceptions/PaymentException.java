package com.restaurante.backend.exceptions;

public class PaymentException extends BusinessException {

    //Error de método de pago nulo

    public PaymentException(String mensaje) {
        super("general", mensaje, "PAYMENT_ERROR");
    }

    public PaymentException(String campo, String mensaje) {
        super(campo, mensaje, "PAYMENT_ERROR");
    }
}
