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
    
    private final CargaConcurrenteMetricaService cargaConcurrenteMetricaService;
    private final AsignacionMesaMetricaService asignacionMetricaService;
    private final ConversionReservaMetricaService conversionMetricaService;
    private final StripeMetricaService stripeMetricaService;
    private final ReservaMapper reservaMapper;
    private final EmailService emailService;
    private final PagoMapper pagoMapper;

    private static final Double PRECIO_POR_PERSONA_USD = 5.0;
    private static final LocalTime HORA_APERTURA = LocalTime.of(12, 00);
    private static final LocalTime HORA_CIERRE = LocalTime.of(23, 59);

    @Transactional
    public ReservaResponseDTO crearReserva(ReservaRequestDTO dto) {

        // MÉTRICA — inicio de procesamiento, registrar concurrencia
        cargaConcurrenteMetricaService.registrarInicio();

        try{
        Reserva reserva = reservaMapper.toEntity(dto);

        LocalTime horaDeseada = dto.getHoraInicio();

        if (horaDeseada.isBefore(HORA_APERTURA)) {
            throw new ValidationException("hora", "Por favor seleccione una hora de reserva entre las 12:00 y las 00:00.");
        }

        if (horaDeseada.isAfter(HORA_CIERRE)) {
            throw new ValidationException("hora", "Por favor seleccione una hora de reserva entre las 12:00 y las 00:00.");
        }

        reserva.setHoraFin(reserva.getHoraInicio().plusHours(2));

        List<Mesa> mesasCandidatas = mesaRepo
        .findByCapacidadGreaterThanEqualOrderByCapacidadAsc(reserva.getNumPersonas());

        // MÉTRICA — medir tiempo del algoritmo de asignación
        long tiempoInicio = System.currentTimeMillis();

        Mesa mesaAsignada = mesasCandidatas.stream()
            .filter(m -> reservaRepo.findOverlappingReservations(
                    m.getIdMesa(), reserva.getFecha(),
                    reserva.getHoraInicio(), reserva.getHoraFin()).isEmpty())
            .findFirst()
            .orElseThrow(() -> new ValidationException("general",
                    "No hay mesas disponibles para " + reserva.getNumPersonas()
                    + " personas en ese horario."));

        long duracionMs = System.currentTimeMillis() - tiempoInicio;
        asignacionMetricaService.registrarAsignacion(duracionMs);

        reserva.setMesa(mesaAsignada);
        
        EstadoReserva estadoPendiente = estadoRepo.findByNombre("PENDIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Estado PENDIENTE no configurado en la base de datos"));
        reserva.setEstadoReserva(estadoPendiente);

        Double montoTotal = reserva.getNumPersonas() * PRECIO_POR_PERSONA_USD;

        Reserva guardada = reservaRepo.save(reserva);

        // MÉTRICA — se registró una intención de reserva
        conversionMetricaService.registrarIntento();

        // MÉTRICA RNF-18 — reserva completada exitosamente bajo carga
        cargaConcurrenteMetricaService.registrarExito();

        enviarCorreoConfirmacion(guardada, montoTotal);
        return reservaMapper.toResponseDTO(guardada);
        } catch (Exception e) {
        // MÉTRICA RNF-18 — fallo bajo carga (libera el contador de concurrencia)
        cargaConcurrenteMetricaService.registrarFallo();
        throw e; // relanzar para que GlobalExceptionHandler lo maneje
        }
    }
    
    public List<ReservaResponseDTO> buscarReservas(LocalDate fecha, LocalTime hora, Integer personas) {
        List<Reserva> entidades = reservaRepo.buscarTodasConFiltros(fecha, hora, personas);
        
        return entidades.stream()
                .map(reservaMapper::toDto)
                .collect(Collectors.toList());
    }

    public int contarMesasDisponibles(LocalDate fecha, LocalTime hora, Integer personas) {
        List<Mesa> todasLasMesas = mesaRepo.findAll();
        
        if (fecha == null) {
            return todasLasMesas.size();
        }
        
        if (hora == null && personas == null) {
            return todasLasMesas.size();
        }
        
        List<Reserva> reservasFecha = reservaRepo.buscarReservasPorFecha(fecha);
        if (hora != null) {
            LocalTime horaFinNuevaReserva = hora.plusHours(2);
            
            final LocalTime horaFinReserva = horaFinNuevaReserva;
            reservasFecha = reservasFecha.stream()
                .filter(r -> r.getHoraInicio().isBefore(horaFinReserva) && r.getHoraFin().isAfter(hora))
                .collect(Collectors.toList());
        }
        
        List<Long> idsMesasOcupadas = reservasFecha.stream()
                .map(r -> r.getMesa().getIdMesa())
                .distinct()
                .collect(Collectors.toList());
        
        if (personas != null) {
            List<Mesa> mesasAdecuadas = todasLasMesas.stream()
                    .filter(m -> m.getCapacidad() >= personas)
                    .collect(Collectors.toList());
            
            return (int) mesasAdecuadas.stream()
                    .filter(m -> !idsMesasOcupadas.contains(m.getIdMesa()))
                    .count();
        }
        
        return (int) todasLasMesas.stream()
                .filter(m -> !idsMesasOcupadas.contains(m.getIdMesa()))
                .count();
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
        List<Reserva> entidades = reservaRepo.buscarTodasPorUsuarioConFiltros(cedula, fecha, hora, personas);
        
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

        // MÉTRICA — registrar intento de comunicación con Stripe
        stripeMetricaService.registrarIntento();

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setIdPasarela(idPasarela);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoPago("succeeded");

        reserva.setEstadoReserva(estadoPagada);

        Pago pagoGuardado = pagoRepo.save(pago);
        reservaRepo.save(reserva);

        // MÉTRICA — si llegó hasta aquí, Stripe respondió bien
        stripeMetricaService.registrarExito();

        // MÉTRICA — la reserva fue pagada exitosamente
        conversionMetricaService.registrarPago();

        emailService.enviarFacturaConPDF(pagoGuardado);

        return pagoMapper.toResponseDTO(pagoGuardado);
    }

    @Transactional
    public ReservaResponseDTO cancelarReserva(Long idReserva) {

        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", idReserva.toString()));

        if ("FINALIZADA".equals(reserva.getEstadoReserva().getNombre())) {
            throw new ValidationException("general", "No se puede cancelar una reserva que ya ha sido finalizada.");
        }

        if (reserva.getFecha().isBefore(LocalDate.now())) {
            throw new ValidationException("general", "No se pueden cancelar reservas de fechas pasadas.");
        }

        // MÉTRICA — si se cancela sin haber pagado, es una reserva abandonada
        boolean eraPendiente = "PENDIENTE".equals(reserva.getEstadoReserva().getNombre());

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

        // MÉTRICA — registrar abandono solo si nunca llegó a pagarse
        if (eraPendiente) {
            conversionMetricaService.registrarAbandonada();
        }

        emailService.enviarCancelacionReserva(
            reserva.getUsuario().getEmail(),
            reserva.getUsuario().getNombre(),
            reserva.getFecha(),
            reserva.getHoraInicio()
        );

        return reservaMapper.toDto(guardada);
    }

    @Transactional
    public ReservaResponseDTO finalizarReserva(Long idReserva) {
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", idReserva.toString()));

        // Verificar que la reserva esté en estado PAGADA
        if (!"PAGADA".equals(reserva.getEstadoReserva().getNombre())) {
            throw new ValidationException("general", "Solo se pueden finalizar reservas que estén en estado PAGADA.");
        }

        EstadoReserva estadoFinalizada = estadoRepo.findByNombre("FINALIZADA")
                .orElseThrow(() -> new ResourceNotFoundException("Estado FINALIZADA no configurado en la base de datos"));

        reserva.setEstadoReserva(estadoFinalizada);
        Reserva guardada = reservaRepo.save(reserva);

        return reservaMapper.toDto(guardada);
    }

    private boolean ejecutarReembolsoStripe(String idPasarela, Double monto) {
        // MÉTRICA — cada reembolso también es un intento con Stripe
        stripeMetricaService.registrarIntento();
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(idPasarela)
                    .build();
            Refund refund = Refund.create(params);
            boolean ok = "succeeded".equals(refund.getStatus());

            // MÉTRICA — resultado del reembolso
            if (ok) {
                stripeMetricaService.registrarExito();
            } else {
                stripeMetricaService.registrarFalloTecnico();
            }
            return ok;
        } catch (StripeException e) {
            // MÉTRICA — excepción técnica = fallo técnico
            stripeMetricaService.registrarFalloTecnico();
            System.err.println("MENSAJE DE STRIPE: " + e.getMessage());
            System.err.println("TIPO DE ERROR: " + e.getCode());
            return false;
        }
    }

    private void enviarCorreoConfirmacion(Reserva reserva, Double montoTotal) {
        emailService.enviarConfirmacionReserva(
            reserva.getUsuario().getEmail(),
            reserva.getUsuario().getNombre(),
            reserva.getFecha(),
            reserva.getHoraInicio(),
            reserva.getNumPersonas(),
            montoTotal
        );
    }
}
