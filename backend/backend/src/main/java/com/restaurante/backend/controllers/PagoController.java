package com.restaurante.backend.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.services.PagoService;
import com.restaurante.backend.services.PasarelaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService; // Inyectamos el servicio, no los repositorios
    private final PasarelaService pasarelaService; // Servicio para interactuar con Stripe

    @PostMapping("/crear-intento") //PUBLICA
    public ResponseEntity<Map<String, String>> iniciarPago(@RequestParam(name = "idReserva") Long idReserva) {
        // 1. Log para confirmar que el dato llegó al servidor
        System.out.println("DEBUG: Recibida solicitud para reserva ID: " + idReserva);

        try {
            // 2. Llamada al servicio que ya modificamos para calcular (personas * 5)
            Map<String, String> datosStripe = pasarelaService.crearIntentoPago(idReserva);
            return ResponseEntity.ok(datosStripe);
        } catch (Exception e) {
            System.err.println("DEBUG: Error creando intento: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/confirmar") //PUBLICA
    public ResponseEntity<PagoResponseDTO> confirmarPago(@RequestBody PagoRequestDTO pagoRequest) {
        // Delegamos toda la lógica al servicio
        PagoResponseDTO respuesta = pagoService.procesarConfirmacionPago(pagoRequest);
        return ResponseEntity.ok(respuesta);
    }
}
