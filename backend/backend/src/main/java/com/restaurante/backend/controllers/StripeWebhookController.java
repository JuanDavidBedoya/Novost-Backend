package com.restaurante.backend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.services.ReservaService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Event.Data;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/webhooks")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final ReservaService reservaService;

    public StripeWebhookController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @SuppressWarnings("deprecation")
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("Error al verificar firma de Stripe: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        } catch (Exception e) {
            System.err.println("Error al construir evento de Stripe: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error procesando evento");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            System.out.println("--- EVENTO RECIBIDO DE STRIPE: PAYMENT INTENT SUCCEEDED ---");

            Data eventData = event.getData();
            PaymentIntent paymentIntent = (PaymentIntent) eventData.getObject();

            if (paymentIntent != null) {
                String idPasarela = paymentIntent.getId();
                Double monto = paymentIntent.getAmount() / 100.0;
                String idReservaStr = paymentIntent.getMetadata().get("idReserva");

                System.out.println("Datos Crudos - Pasarela: " + idPasarela + ", Reserva: " + idReservaStr);

                if (idReservaStr != null) {
                    try {
                        reservaService.procesarPagoReserva(Long.parseLong(idReservaStr), idPasarela, monto);
                        System.out.println("¡ÉXITO! Reserva actualizada.");
                    } catch (Exception e) {
                        System.err.println(" ERROR EN SERVICE: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("Error crítico: No se pudo obtener el PaymentIntent del JSON.");
            }
        }

        return ResponseEntity.ok("Evento procesado correctamente");
    }
}
