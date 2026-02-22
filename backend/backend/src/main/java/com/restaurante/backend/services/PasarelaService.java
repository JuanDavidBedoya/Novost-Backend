package com.restaurante.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class PasarelaService {

    @Value("${stripe.key.secret}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String crearIntentoPago(Double monto, String moneda, Long idReserva) throws StripeException {
        // Stripe maneja los montos en centavos (ej: 10.00 USD son 1000 centavos)
        long montoCentavos = (long) (monto * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montoCentavos)
                .setCurrency(moneda)
                .putMetadata("idReserva", String.valueOf(idReserva))
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        
        // Retornamos el client_secret para que el frontend pueda mostrar el formulario de tarjeta
        return intent.getClientSecret();
    }
}
