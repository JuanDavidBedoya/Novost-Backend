package com.restaurante.backend.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurante.backend.dtos.PedidoRequestDTO;
import com.restaurante.backend.entities.Plato;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.exceptions.PaymentException;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.repositories.PlatoRepository;
import com.restaurante.backend.repositories.ReservaRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasarelaService {

    @Value("${stripe.key.secret}")
    private String stripeSecretKey;

    private final ReservaRepository reservaRepo;
    private final PlatoRepository platoRepo;

    private static final Double PRECIO_POR_PERSONA = 5.0;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public Map<String, String> crearIntentoPago(Long idReserva) {
        try {
            Reserva reserva = reservaRepo.findById(idReserva)
                    .orElseThrow(() -> new ResourceNotFoundException("Reserva", idReserva.toString()));

            long montoCentavos = (long) (reserva.getNumPersonas() * PRECIO_POR_PERSONA * 100);

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

        } catch (PaymentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("general", "Error al procesar el pago: " + e.getMessage());
        }
    }

    public Map<String, String> crearIntentoPagoPedido(Long idPedido, Double monto) {
        try {
            long montoCentavos = (long) (monto * 100);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(montoCentavos)
                    .setCurrency("usd")
                    .putMetadata("idPedido", String.valueOf(idPedido))
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
        } catch (Exception e) {
            throw new PaymentException("general", "Error al procesar el pago: " + e.getMessage());
        }
    }

    public Map<String, String> crearIntentoPagoPedidoPrevio(PedidoRequestDTO pedidoRequest) {
        try {
            double subtotal = pedidoRequest.getDetalles().stream()
                    .mapToDouble(detalle -> {
                        Plato plato = platoRepo.findById(detalle.getIdPlato())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Plato", detalle.getIdPlato().toString()));
                        return plato.getPrecioPlato() * detalle.getCantidad();
                    })
                    .sum();

            double total = subtotal + (subtotal * 0.19);
            long montoCentavos = Math.round(total * 100);

            ObjectMapper mapper = new ObjectMapper();
            String pedidoPayloadJson = mapper.writeValueAsString(pedidoRequest);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(montoCentavos)
                    .setCurrency("usd")
                    .putMetadata("pedidoPayload", pedidoPayloadJson)
                    .putMetadata("tipoPago", "PEDIDO_LINEA")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            return Map.of(
                    "clientSecret", intent.getClientSecret(),
                    "idPasarela",   intent.getId()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error creando intento previo de pago: " + e.getMessage(), e);
        }
    }
}