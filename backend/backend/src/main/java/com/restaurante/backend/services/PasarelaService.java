package com.restaurante.backend.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.exceptions.PaymentException;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.repositories.ReservaRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class PasarelaService {

    @Value("${stripe.key.secret}")
    private String stripeSecretKey;

    @Autowired
    private ReservaRepository reservaRepo;
    private static final Double PRECIO_POR_PERSONA = 5.0;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public Map<String, String> crearIntentoPago(Long idReserva) {
        try {
            Reserva reserva = reservaRepo.findById(idReserva)
                    .orElseThrow(() -> new ResourceNotFoundException("Reserva", idReserva.toString()));

            Double montoCalculado = reserva.getNumPersonas() * PRECIO_POR_PERSONA;

            long montoCentavos = (long) (montoCalculado * 100);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(montoCentavos)
                    .setCurrency("usd")
                    .putMetadata("idReserva", String.valueOf(idReserva))
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    )
                    .build();

            PaymentIntent intent;
            try {
                intent = PaymentIntent.create(params);
            } catch (StripeException e) {
                throw new PaymentException("general", "No se pudo inicializar el pago: " + e.getMessage());
            }
            
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("idPasarela", intent.getId());
            respuesta.put("clientSecret", intent.getClientSecret());

            return respuesta;

        } catch (PaymentException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("general", "Error al procesar el pago: " + e.getMessage());
        }
    }
}
