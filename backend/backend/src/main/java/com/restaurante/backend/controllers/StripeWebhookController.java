package com.restaurante.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurante.backend.dtos.PedidoRequestDTO;
import com.restaurante.backend.services.AuditService;
import com.restaurante.backend.services.PedidoService;
import com.restaurante.backend.services.ReservaService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final ReservaService reservaService;
    private final PedidoService pedidoService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public StripeWebhookController(ReservaService reservaService,
                                   PedidoService pedidoService,
                                   ObjectMapper objectMapper,
                                   AuditService auditService) {
        this.reservaService = reservaService;
        this.pedidoService  = pedidoService;
        this.objectMapper   = objectMapper;
        this.auditService   = auditService;
    }

    @SuppressWarnings("deprecation")
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("Firma de Stripe inválida: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        } catch (Exception e) {
            System.err.println("Error construyendo evento Stripe: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error procesando evento");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            System.out.println("--- STRIPE: payment_intent.succeeded ---");

            PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
            if (paymentIntent == null) {
                System.err.println("PaymentIntent nulo en el evento.");
                return ResponseEntity.ok("Evento ignorado");
            }

            String idPasarela    = paymentIntent.getId();
            Double monto         = paymentIntent.getAmount() / 100.0;
            String idReservaStr  = paymentIntent.getMetadata().get("idReserva");
            String idPedidoStr   = paymentIntent.getMetadata().get("idPedido");
            String tipoPago      = paymentIntent.getMetadata().get("tipoPago");
            String pedidoPayload = paymentIntent.getMetadata().get("pedidoPayload");

            // ── Caso 1: Pago de RESERVA (flujo existente) ──────────────────
            if (idReservaStr != null) {
                try {
                    reservaService.procesarPagoReserva(Long.parseLong(idReservaStr), idPasarela, monto);

                    auditService.logActualizacion(AuditService.ENTIDAD_RESERVA, Long.parseLong(idReservaStr),
                        "Pago de reserva confirmado por Stripe (webhook)", null,
                        "estadoPago: PAGADO, idPasarela: " + idPasarela);

                    System.out.println("Reserva #" + idReservaStr + " pagada.");
                } catch (Exception e) {
                    System.err.println("Error procesando pago de reserva: " + e.getMessage());
                }

            // ── Caso 2: Pago de PEDIDO ya creado (flujo legacy) ────────────
            } else if (idPedidoStr != null) {
                try {
                    pedidoService.confirmarPagoPedido(Long.parseLong(idPedidoStr), idPasarela);

                    auditService.logActualizacion(AuditService.ENTIDAD_PEDIDO, Long.parseLong(idPedidoStr),
                        "Pago de pedido confirmado por Stripe (webhook)", null,
                        "estadoPago: PAGADO, idPasarela: " + idPasarela);

                    System.out.println("Pedido #" + idPedidoStr + " confirmado.");
                } catch (Exception e) {
                    System.err.println("Error confirmando pago de pedido: " + e.getMessage());
                }

            // ── Caso 3: NUEVO FLUJO — pedido aún no creado ─────────────────
            // El payload completo del pedido viaja en los metadatos de Stripe.
            // Solo llegamos aquí si el pago fue exitoso: creamos el pedido ahora.
            } else if ("PEDIDO_LINEA".equals(tipoPago) && pedidoPayload != null) {
                try {
                    PedidoRequestDTO pedidoRequest =
                            objectMapper.readValue(pedidoPayload, PedidoRequestDTO.class);

                    // Aseguramos que el método de pago refleje el canal online
                    pedidoRequest.setMetodoPago("LINEA");

                    // Crear el pedido y confirmar el pago en una sola transacción
                    pedidoService.crearYConfirmarPedidoLinea(pedidoRequest, idPasarela);

                    auditService.logCreacion(AuditService.ENTIDAD_PEDIDO, null,
                        "Pedido en línea creado y confirmado vía Stripe (webhook), idPasarela: " + idPasarela);

                    System.out.println("Pedido en línea creado y confirmado. Pasarela: " + idPasarela);
                } catch (Exception e) {
                    System.err.println("Error creando pedido desde webhook: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return ResponseEntity.ok("Evento procesado correctamente");
    }
}