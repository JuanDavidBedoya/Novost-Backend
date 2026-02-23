package com.restaurante.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.ReservaConfirmarPagoRequestDTO;
import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.services.ReservaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> reservar(@RequestBody ReservaRequestDTO reservaRequest) {
        // El service ahora se encarga de convertir el DTO a entidad y viceversa
        ReservaResponseDTO nuevaReserva = reservaService.crearReserva(reservaRequest);
        return ResponseEntity.ok(nuevaReserva);
    }

    // Confirma el pago que viene de la pasarela externa
    @PostMapping("/{id}/confirmar-pago")
    public ResponseEntity<PagoResponseDTO> confirmar(
            @PathVariable Long id, 
            @RequestBody ReservaConfirmarPagoRequestDTO pagoRequest) {
        
        // El service procesa el pago, genera la factura por email y actualiza la reserva
        PagoResponseDTO pagoRealizado = reservaService.procesarPagoReserva(
            id, 
            pagoRequest.getIdPasarela(), 
            pagoRequest.getMonto()
        );
        
        return ResponseEntity.ok(pagoRealizado);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelarReserva(@PathVariable Long id) {
        // Llamamos al servicio para ejecutar la lógica de negocio
        ReservaResponseDTO response = reservaService.cancelarReserva(id);
        
        // Retornamos la reserva actualizada con su nuevo estado
        return ResponseEntity.ok(response);
    }
}
