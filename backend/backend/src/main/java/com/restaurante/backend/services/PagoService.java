package com.restaurante.backend.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.mappers.PagoMapper;
import com.restaurante.backend.repositories.EstadoReservaRepository;
import com.restaurante.backend.repositories.PagoRepository;
import com.restaurante.backend.repositories.ReservaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepo;
    private final ReservaRepository reservaRepo;
    private final EstadoReservaRepository estadoRepo;
    private final PagoMapper pagoMapper;
    private final EmailService emailService;

    @Transactional
    public PagoResponseDTO procesarConfirmacionPago(PagoRequestDTO dto) {
        // 1. Buscar la reserva
        Reserva reserva = reservaRepo.findById(dto.getIdReserva())
                .orElseThrow(() -> new RuntimeException("Reserva no existe con ID: " + dto.getIdReserva()));

        // 2. Crear y guardar el Pago usando el mapper
        Pago pago = pagoMapper.toEntity(dto);
        pago.setEstadoPago("succeeded");
        pago.setFechaPago(LocalDateTime.now());
        Pago pagoGuardado = pagoRepo.save(pago);

        // 3. Actualizar el estado de la reserva
        EstadoReserva estadoPagada = estadoRepo.findByNombre("PAGADA")
                .orElseThrow(() -> new RuntimeException("Estado PAGADA no configurado"));
        reserva.setEstadoReserva(estadoPagada);
        reservaRepo.save(reserva);

        // 4. Enviar la factura por correo (Aprovechamos la lógica que ya creamos)
        emailService.enviarFactura(pagoGuardado);

        // 5. Retornar el DTO de respuesta
        return pagoMapper.toResponseDTO(pagoGuardado);
    }
}
