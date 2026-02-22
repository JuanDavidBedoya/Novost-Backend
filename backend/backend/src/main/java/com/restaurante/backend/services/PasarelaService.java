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

    /**
     * Crea un PaymentIntent en Stripe y devuelve la información necesaria
     * para que el frontend complete el pago.
     */
    public String crearIntentoPago(Double monto, String moneda, Long idReserva) {
        try {
            // 1. Validación de seguridad básica
            if (monto <= 0) {
                throw new RuntimeException("El monto debe ser mayor a cero");
            }

            // 2. Stripe maneja montos en centavos (ej: 10.50 -> 1050)
            long montoCentavos = (long) (monto * 100);

            // 3. Configuración de parámetros para Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(montoCentavos)
                    .setCurrency(moneda.toLowerCase())
                    .putMetadata("idReserva", String.valueOf(idReserva))
                    // Habilitar métodos de pago automáticos (tarjetas, wallets, etc)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    )
                    .build();

            // 4. Llamada a la API de Stripe
            PaymentIntent intent = PaymentIntent.create(params);
            
            // Retornamos el client_secret, que es la "llave" para que el frontend
            // pueda renderizar el formulario de pago de forma segura.
            return intent.getClientSecret();

        } catch (StripeException e) {
            System.err.println("Error al comunicarse con Stripe: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar el pago en la pasarela externa");
        }
    }
}
