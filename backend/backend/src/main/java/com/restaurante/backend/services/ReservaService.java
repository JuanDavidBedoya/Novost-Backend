package com.restaurante.backend.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private static final LocalTime HORA_APERTURA = LocalTime.of(12, 00); // 12:00 PM
    private static final LocalTime HORA_CIERRE = LocalTime.of(23, 59); // 11:59 PM

    @Transactional
    public ReservaResponseDTO crearReserva(ReservaRequestDTO dto) {
        // 1. DTO -> Entidad (El mapper ya busca al Usuario por cédula)
        Reserva reserva = reservaMapper.toEntity(dto);

        LocalTime horaDeseada = dto.getHoraInicio();

        if (horaDeseada.isBefore(HORA_APERTURA)) {
            throw new RuntimeException("Por favor seleccione una hora de reserva entre las 12:00 y las 00:00.");
        }

        if (horaDeseada.isAfter(HORA_CIERRE)) {
            throw new RuntimeException("Por favor seleccione una hora de reserva entre las 12:00 y las 00:00.");
        }

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

        String emailDestino = guardada.getUsuario().getEmail();
        String nombreCliente = guardada.getUsuario().getNombre();
        LocalDate fecha = guardada.getFecha();
        LocalTime horaInicio = guardada.getHoraInicio(); // Obtenemos la hora

        String cuerpo = String.format(
            "Hola %s, su reserva ha sido registrada.\n\n" +
            "Detalles de su reserva:\n" +
            "- Fecha: %s\n" +
            "- Hora de inicio: %s\n\n" + 
            "Recuerde que tiene hasta 24 horas antes para realizar el pago y confirmar su asistencia.",
            nombreCliente, fecha, horaInicio, fecha
        );

        // Enviamos al correo real del usuario
        emailService.enviarCorreo(emailDestino, "Confirmación de Reserva - Novost", cuerpo);

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
    public ReservaResponseDTO cancelarReserva(Long idReserva) {
        // 1. Buscar la reserva
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // 2. Validación de fecha
        if (reserva.getFecha().isBefore(LocalDate.now())) {
            throw new RuntimeException("No se pueden cancelar reservas de fechas pasadas.");
        }

        // 3. Procesar Reembolso si existe un pago asociado
        pagoRepo.findByReserva(reserva).ifPresent(pago -> {
            // Suponiendo que tienes esta lógica o el servicio de pasarela inyectado
            boolean reembolsoExitoso = ejecutarReembolsoStripe(pago.getIdPasarela(), pago.getMonto());
            
            if (!reembolsoExitoso) {
                throw new RuntimeException("Error al procesar la devolución en Stripe");
            }
            
            pago.setIdEstadoPago("REEMBOLSADO");
            pagoRepo.save(pago);
        });

        // 4. Cambiar estado a CANCELADA
        EstadoReserva estadoCancelado = estadoRepo.findByNombre("CANCELADA")
                .orElseThrow(() -> new RuntimeException("Estado CANCELADA no configurado en la base de datos"));
        
        reserva.setEstadoReserva(estadoCancelado);
        Reserva guardada = reservaRepo.save(reserva);

        // 5. Notificar por correo al usuario real
        String cuerpoEmail = String.format(
            "Hola %s, le confirmamos que su reserva para el día %s a las %s ha sido cancelada.\n" +
            "Si realizó un pago previo, el reembolso ha sido solicitado a su entidad bancaria.",
            reserva.getUsuario().getNombre(), 
            reserva.getFecha(), 
            reserva.getHoraInicio()
        );
        
        emailService.enviarCorreo(reserva.getUsuario().getEmail(), "Cancelación de Reserva - Novost", cuerpoEmail);

        // 6. Retornar el DTO usando tu mapper
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
}