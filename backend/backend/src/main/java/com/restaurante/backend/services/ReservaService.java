package com.restaurante.backend.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Mesa;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.exceptions.PaymentException;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.exceptions.ValidationException;
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

    private final ReservaMapper reservaMapper;
    private final EmailService emailService;
    private final PagoMapper pagoMapper;

    private static final Double PRECIO_POR_PERSONA_USD = 5.0;
    private static final LocalTime HORA_APERTURA = LocalTime.of(12, 00);
    private static final LocalTime HORA_CIERRE = LocalTime.of(23, 59);

    @Transactional
    public ReservaResponseDTO crearReserva(ReservaRequestDTO dto) {
        Reserva reserva = reservaMapper.toEntity(dto);

        LocalTime horaDeseada = dto.getHoraInicio();

        if (horaDeseada.isBefore(HORA_APERTURA)) {
            throw new ValidationException("hora", "Por favor seleccione una hora de reserva entre las 12:00 y las 00:00.");
        }

        if (horaDeseada.isAfter(HORA_CIERRE)) {
            throw new ValidationException("hora", "Por favor seleccione una hora de reserva entre las 12:00 y las 00:00.");
        }

        reserva.setHoraFin(reserva.getHoraInicio().plusHours(2));

        List<Mesa> mesasCandidatas = mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(reserva.getNumPersonas());

        Mesa mesaAsignada = mesasCandidatas.stream()
            .filter(m -> reservaRepo.findOverlappingReservations(
                    m.getIdMesa(), reserva.getFecha(), 
                    reserva.getHoraInicio(), reserva.getHoraFin()).isEmpty())
            .findFirst()
            .orElseThrow(() -> new ValidationException("general", "No hay mesas disponibles para " + reserva.getNumPersonas() + " personas en ese horario."));

        reserva.setMesa(mesaAsignada);
        
        EstadoReserva estadoPendiente = estadoRepo.findByNombre("PENDIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Estado PENDIENTE no configurado en la base de datos"));
        reserva.setEstadoReserva(estadoPendiente);

        Double montoTotal = reserva.getNumPersonas() * PRECIO_POR_PERSONA_USD;

        Reserva guardada = reservaRepo.save(reserva);

        enviarCorreoConfirmacion(guardada, montoTotal);

        return reservaMapper.toResponseDTO(guardada);
    }
    
    public List<ReservaResponseDTO> buscarReservas(LocalDate fecha, LocalTime hora, Integer personas) {
        List<Reserva> entidades = reservaRepo.buscarConFiltros(fecha, hora, personas);
        
        return entidades.stream()
                .map(reservaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ReservaResponseDTO> buscarReservasPorUsuario(String cedula) {
        List<Reserva> entidades = reservaRepo.findByUsuarioCedula(cedula);
        
        return entidades.stream()
                .map(reservaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ReservaResponseDTO> buscarReservasPorUsuarioConFiltros(String cedula, LocalDate fecha, LocalTime hora, Integer personas) {
        List<Reserva> entidades = reservaRepo.buscarPorUsuarioConFiltros(cedula, fecha, hora, personas);
        
        return entidades.stream()
                .map(reservaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PagoResponseDTO procesarPagoReserva(Long idReserva, String idPasarela, Double monto) {
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", idReserva.toString()));

        Double montoEsperado = reserva.getNumPersonas() * PRECIO_POR_PERSONA_USD;
        
        if (Math.abs(monto - montoEsperado) > 0.01) {
             System.out.println(" Alerta: El monto pagado ($" + monto + ") no coincide con el esperado ($" + montoEsperado + ")");
        }

        EstadoReserva estadoPagada = estadoRepo.findByNombre("PAGADA")
                .orElseThrow(() -> new ResourceNotFoundException("Estado PAGADA no configurado en la base de datos"));

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setIdPasarela(idPasarela);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoPago("succeeded"); 

        reserva.setEstadoReserva(estadoPagada);

        Pago pagoGuardado = pagoRepo.save(pago);
        reservaRepo.save(reserva);

        emailService.enviarFacturaConPDF(pagoGuardado);

        return pagoMapper.toResponseDTO(pagoGuardado);
    }

    @Transactional
    public ReservaResponseDTO cancelarReserva(Long idReserva) {

        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", idReserva.toString()));

        if (reserva.getFecha().isBefore(LocalDate.now())) {
            throw new ValidationException("general", "No se pueden cancelar reservas de fechas pasadas.");
        }

        pagoRepo.findByReserva(reserva).ifPresent(pago -> {

            boolean reembolsoExitoso = ejecutarReembolsoStripe(pago.getIdPasarela(), pago.getMonto());
            
            if (!reembolsoExitoso) {
                throw new PaymentException("general", "Error al procesar la devolución en Stripe");
            }
            
            pago.setEstadoPago("refunded");
            pagoRepo.save(pago);
        });

        EstadoReserva estadoCancelado = estadoRepo.findByNombre("CANCELADA")
                .orElseThrow(() -> new ResourceNotFoundException("Estado CANCELADA no configurado en la base de datos"));
        
        reserva.setEstadoReserva(estadoCancelado);
        Reserva guardada = reservaRepo.save(reserva);

        String cuerpoEmail = String.format(
            "Hola %s, le confirmamos que su reserva para el día %s a las %s ha sido cancelada.\n" +
            "Si realizó un pago previo, el reembolso ha sido solicitado a su entidad bancaria.",
            reserva.getUsuario().getNombre(), 
            reserva.getFecha(), 
            reserva.getHoraInicio()
        );
        
        emailService.enviarCorreo(reserva.getUsuario().getEmail(), "Cancelación de Reserva - Novost", cuerpoEmail);

        return reservaMapper.toDto(guardada);
    }

    private boolean ejecutarReembolsoStripe(String idPasarela, Double monto) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(idPasarela)
                    .build();
            Refund refund = Refund.create(params);
            return "succeeded".equals(refund.getStatus());
        } catch (StripeException e) {
            System.err.println("MENSAJE DE STRIPE: " + e.getMessage());
            System.err.println("TIPO DE ERROR: " + e.getCode());
            return false;
        }
    }

    private void enviarCorreoConfirmacion(Reserva reserva, Double montoTotal) {
        String cuerpo = String.format(
            "Hola %s, su reserva ha sido registrada.\n\n" +
            "Detalles de su reserva:\n" +
            "- Fecha: %s\n" +
            "- Hora: %s\n" +
            "- Personas: %d\n" +
            "- Valor a pagar: $%s USD\n\n" + 
            "Recuerde realizar el pago para confirmar su asistencia.",
            reserva.getUsuario().getNombre(), 
            reserva.getFecha(), 
            reserva.getHoraInicio(),
            reserva.getNumPersonas(),
            montoTotal
        );
        emailService.enviarCorreo(reserva.getUsuario().getEmail(), "Confirmación de Reserva - Novost", cuerpo);
    }
}
