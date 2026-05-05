package com.restaurante.backend.mappers;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.repositories.ReservaRepository;

import lombok.RequiredArgsConstructor;

// Mapper para convertir entre DTO y entidad Pago

@Component
@RequiredArgsConstructor
public class PagoMapper {

    // Inyección de dependencia: repositorio de reservas para obtener datos de la reserva

    private final ReservaRepository reservaRepo;

    // Método toEntity: convierte PagoRequestDTO a entidad Pago, obtiene reserva y asigna timestamp

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

    // Método toResponseDTO: transforma entidad Pago en PagoResponseDTO con datos de pago e información de reserva

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
