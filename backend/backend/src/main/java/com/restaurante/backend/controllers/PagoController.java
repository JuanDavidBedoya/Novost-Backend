package com.restaurante.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.services.PagoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService; // Inyectamos el servicio, no los repositorios

    @PostMapping("/confirmar")
    public ResponseEntity<PagoResponseDTO> confirmarPago(@RequestBody PagoRequestDTO pagoRequest) {
        // Delegamos toda la lógica al servicio
        PagoResponseDTO respuesta = pagoService.procesarConfirmacionPago(pagoRequest);
        return ResponseEntity.ok(respuesta);
    }
}
