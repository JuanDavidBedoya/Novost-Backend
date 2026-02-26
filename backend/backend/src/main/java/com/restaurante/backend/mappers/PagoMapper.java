package com.restaurante.backend.mappers;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.repositories.ReservaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PagoMapper {

    private final ReservaRepository reservaRepo;

    public Pago toEntity(PagoRequestDTO dto) {
        if (dto == null) return null;
        Pago pago = new Pago();
        pago.setReserva(reservaRepo.findById(dto.getIdReserva())
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada para el pago")));
        pago.setIdPasarela(dto.getIdPasarela());
        pago.setMonto(dto.getMonto());
        pago.setFechaPago(LocalDateTime.now());
        return pago;
    }

    public PagoResponseDTO toResponseDTO(Pago pago) {
        if (pago == null) return null;
        return new PagoResponseDTO(
            pago.getIdPago(),
            pago.getReserva().getIdReserva(),
            pago.getIdPasarela(),
            pago.getEstadoPago(),
            pago.getFechaPago(),
            pago.getMonto()
        );
    }
}
