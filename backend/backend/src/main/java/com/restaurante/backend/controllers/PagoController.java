package com.restaurante.backend.controllers;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.repositories.PagoRepository;
import com.restaurante.backend.repositories.ReservaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoRepository pagoRepo;
    private final ReservaRepository reservaRepo;

    // Este endpoint lo llamarás desde tu frontend cuando Stripe diga "Pago Exitoso"
    @PostMapping("/confirmar")
    @Transactional
    public ResponseEntity<String> confirmarPago(@RequestBody Map<String, Object> payload) {
        Long idReserva = Long.parseLong(payload.get("idReserva").toString());
        String idPasarela = payload.get("idPasarela").toString(); // El ID de Stripe
        Double monto = Double.parseDouble(payload.get("monto").toString());

        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no existe"));

        // 1. Crear el registro en la tabla Pago
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setIdPasarela(idPasarela);
        pago.setIdEstadoPago("succeeded");
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pagoRepo.save(pago);

        // 2. Cambiar el estado de la reserva a "PAGADA" o "CONFIRMADA"
        reserva.getEstadoReserva().setNombre("PAGADA");
        reservaRepo.save(reserva);

        return ResponseEntity.ok("Pago registrado y reserva confirmada");
    }
}
