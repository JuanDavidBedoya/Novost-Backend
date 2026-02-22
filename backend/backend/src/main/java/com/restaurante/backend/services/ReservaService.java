package com.restaurante.backend.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Mesa;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.mappers.PagoMapper;
import com.restaurante.backend.mappers.ReservaMapper;
import com.restaurante.backend.repositories.EstadoReservaRepository;
import com.restaurante.backend.repositories.MesaRepository;
import com.restaurante.backend.repositories.PagoRepository;
import com.restaurante.backend.repositories.ReservaRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final PagoRepository pagoRepo;
    private final MesaRepository mesaRepo;
    private final EstadoReservaRepository estadoRepo;

    // Inyectamos los mappers individuales
    private final ReservaMapper reservaMapper;
    private final PagoMapper pagoMapper;
    private final EmailService emailService;

    @Transactional
    public ReservaResponseDTO crearReserva(ReservaRequestDTO dto) {
        // 1. DTO -> Entidad (El mapper ya busca al Usuario por cédula)
        Reserva reserva = reservaMapper.toEntity(dto);

        // 2. Lógica de negocio: Calcular hora fin (Inicio + 2 horas)
        reserva.setHoraFin(reserva.getHoraInicio().plusHours(2));

        // 3. Buscar mesas que tengan capacidad suficiente
        List<Mesa> mesasCandidatas = mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(reserva.getNumPersonas());

        // 4. Encontrar la primera mesa disponible (Uso de Java Stream)
        Mesa mesaAsignada = mesasCandidatas.stream()
            .filter(m -> reservaRepo.findOverlappingReservations(
                    m.getIdMesa(), reserva.getFecha(), 
                    reserva.getHoraInicio(), reserva.getHoraFin()).isEmpty())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hay mesas disponibles para " + reserva.getNumPersonas() + " personas en ese horario."));

        reserva.setMesa(mesaAsignada);
        
        // 5. Asignar estado inicial (Busca en la BD el objeto EstadoReserva)
        EstadoReserva estadoPendiente = estadoRepo.findByNombre("PENDIENTE")
                .orElseThrow(() -> new RuntimeException("Estado PENDIENTE no configurado"));
        reserva.setEstadoReserva(estadoPendiente);

        Reserva guardada = reservaRepo.save(reserva);

        // 6. Enviar correo de confirmación
        emailService.enviarCorreo(
            guardada.getUsuario().getEmail(),
            "Reserva Recibida - Novost",
            "Hola " + guardada.getUsuario().getNombre() + ", su reserva ha sido registrada.\n" +
            "Recuerde que tiene hasta 24 horas antes del " + guardada.getFecha() + " para pagar."
        );

        // 7. Entidad -> DTO de respuesta
        return reservaMapper.toResponseDTO(guardada);
    }

    @Transactional
    public PagoResponseDTO procesarPagoReserva(Long idReserva, String idPasarela, Double monto) {
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Creamos el objeto Pago
        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setIdPasarela(idPasarela);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setIdEstadoPago("succeeded"); 

        // Actualizamos el estado de la reserva
        EstadoReserva estadoPagada = estadoRepo.findByNombre("PAGADA")
                .orElseThrow(() -> new RuntimeException("Estado PAGADA no configurado"));
        reserva.setEstadoReserva(estadoPagada);
        reservaRepo.save(reserva);

        Pago pagoGuardado = pagoRepo.save(pago);
        
        // Enviar factura
        emailService.enviarFactura(pagoGuardado);

        return pagoMapper.toResponseDTO(pagoGuardado);
    }

    @Transactional
    public void cancelarReserva(Long idReserva) {
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (reserva.getFecha().isBefore(LocalDate.now())) {
            throw new RuntimeException("No se pueden cancelar ni reembolsar reservas de fechas pasadas.");
        }

        // 1. Verificar y procesar reembolso si existe pago
        pagoRepo.findByReserva(reserva).ifPresent(pago -> {
            boolean reembolsoExitoso = ejecutarReembolsoStripe(pago.getIdPasarela(), pago.getMonto());
            if (!reembolsoExitoso) {
                throw new RuntimeException("Error al procesar la devolución en Stripe");
            }
            pago.setIdEstadoPago("REEMBOLSADO");
            pagoRepo.save(pago);
        });

        // 2. Cambiar estado a CANCELADA
        EstadoReserva estadoCancelado = estadoRepo.findByNombre("CANCELADA")
                .orElseThrow(() -> new RuntimeException("Estado CANCELADA no configurado"));
        
        reserva.setEstadoReserva(estadoCancelado);
        reservaRepo.save(reserva);

        // 3. Notificar por correo
        emailService.enviarCorreo(reserva.getUsuario().getEmail(), "Reserva Cancelada", "Su reserva ha sido cancelada y el reembolso procesado.");
    }

    private boolean ejecutarReembolsoStripe(String idPasarela, Double monto) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(idPasarela)
                    .build();
            Refund refund = Refund.create(params);
            return "succeeded".equals(refund.getStatus());
        } catch (StripeException e) {
            return false;
        }
    }
}