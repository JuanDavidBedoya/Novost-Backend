package com.restaurante.backend.components;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.repositories.EstadoReservaRepository;
import com.restaurante.backend.repositories.ReservaRepository;
import com.restaurante.backend.services.EmailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservaTask {

    private final ReservaRepository reservaRepo;
    private final EstadoReservaRepository estadoRepo;
    private final EmailService emailService;

    /**
     * Recordatorio 12 horas antes.
     * Se ejecuta cada hora para revisar qué reservas inician en el bloque de 12 horas a futuro.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void enviarRecordatorios12Horas() {
        LocalDateTime ahoraMas12 = LocalDateTime.now().plusHours(12);
        LocalDate fechaBusqueda = ahoraMas12.toLocalDate();
        
        // Ventana de captura (para asegurar que no se salte ninguna por segundos)
        LocalTime inicioVentana = ahoraMas12.toLocalTime().withMinute(0).withSecond(0);
        LocalTime finVentana = inicioVentana.plusHours(1);

        List<Reserva> proximas = reservaRepo.findReservasParaRecordatorio(fechaBusqueda, inicioVentana, finVentana);

        proximas.forEach(r -> {
            // Usamos el método especializado del EmailService
            emailService.enviarRecordatorio(
                r.getUsuario().getEmail(),
                r.getUsuario().getNombre(),
                r.getFecha(),
                r.getHoraInicio()
            );
        });
    }

    /**
     * Cancelación automática por impago.
     * Se ejecuta cada hora para cancelar reservas que están a menos de 24h de iniciar y siguen PENDIENTES.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cancelarReservasNoPagadas() {
        // Buscamos el objeto estado "CANCELADA" de la base de datos
        EstadoReserva estadoCancelado = estadoRepo.findByNombre("CANCELADA")
                .orElseThrow(() -> new RuntimeException("Estado CANCELADA no configurado en BD"));

        // Límite: Reservas que ocurren mañana (a 24h de distancia)
        LocalDate fechaLimite = LocalDate.now().plusDays(1);
        List<Reserva> vencidas = reservaRepo.findReservasNoPagadasVencidas(fechaLimite);

        vencidas.forEach(r -> {
            r.setEstadoReserva(estadoCancelado);
            
            // Usamos el método de notificación de cancelación del EmailService
            emailService.enviarNotificacionCancelacion(
                r.getUsuario().getEmail(),
                r.getUsuario().getNombre(),
                "Falta de pago dentro del plazo requerido (24 horas antes del evento)."
            );
        });
        
        // Guardamos los cambios en lote
        reservaRepo.saveAll(vencidas);
    }
}
