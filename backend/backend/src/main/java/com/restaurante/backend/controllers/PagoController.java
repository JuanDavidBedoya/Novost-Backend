package com.restaurante.backend.controllers;

import java.util.Map;

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

    @PostMapping("/crear-intento")
    public ResponseEntity<Map<String, String>> iniciarPago(@RequestParam Long idReserva, @RequestParam Double monto) {
        // Ahora recibimos el mapa con los dos IDs
        Map<String, String> datosStripe = pasarelaService.crearIntentoPago(monto, "usd", idReserva);
        
        // Aquí podrías incluso guardar ya el idPasarela en tu base de datos si quisieras
        return ResponseEntity.ok(datosStripe);
    }

    @PostMapping("/confirmar")
    public ResponseEntity<PagoResponseDTO> confirmarPago(@RequestBody PagoRequestDTO pagoRequest) {
        // Delegamos toda la lógica al servicio
        PagoResponseDTO respuesta = pagoService.procesarConfirmacionPago(pagoRequest);
        return ResponseEntity.ok(respuesta);
    }
}
