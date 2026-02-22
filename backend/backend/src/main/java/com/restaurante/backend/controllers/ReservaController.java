package com.restaurante.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.services.ReservaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    // Crea la reserva (Estado inicial = Pendiente de pago)
    @PostMapping
    public ResponseEntity<Reserva> reservar(@RequestBody Reserva reserva) {
        return ResponseEntity.ok(reservaService.crearReserva(reserva));
    }

    // Confirma el pago que viene de la pasarela externa
    @PostMapping("/{id}/confirmar-pago")
    public ResponseEntity<Pago> confirmar(@PathVariable Long id, @RequestParam String idPasarela, @RequestParam Double monto) {
        return ResponseEntity.ok(reservaService.procesarPagoReserva(id, idPasarela, monto));
    }
}
