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

    @Scheduled(cron = "0 0 * * * *")
    public void enviarRecordatorios12Horas() {
        LocalDateTime ahoraMas12 = LocalDateTime.now().plusHours(12);
        LocalDate fechaBusqueda = ahoraMas12.toLocalDate();
        
        LocalTime inicioVentana = ahoraMas12.toLocalTime().withMinute(0).withSecond(0);
        LocalTime finVentana = inicioVentana.plusHours(1);

        List<Reserva> proximas = reservaRepo.findReservasParaRecordatorio(fechaBusqueda, inicioVentana, finVentana);

        proximas.forEach(r -> {
            emailService.enviarRecordatorio(
                r.getUsuario().getEmail(),
                r.getUsuario().getNombre(),
                r.getFecha(),
                r.getHoraInicio()
            );
        });
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cancelarReservasNoPagadas() {
        LocalDateTime limiteDePagoYaVencido = LocalDateTime.now().plusHours(24);
        
        LocalDate fechaRef = limiteDePagoYaVencido.toLocalDate();
        LocalTime horaRef = limiteDePagoYaVencido.toLocalTime();

        List<Reserva> vencidas = reservaRepo.findReservasVencidas(fechaRef, horaRef);

        if (!vencidas.isEmpty()) {
            EstadoReserva estadoCancelado = estadoRepo.findByNombre("CANCELADA")
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado"));

            vencidas.forEach(r -> r.setEstadoReserva(estadoCancelado));
            reservaRepo.saveAll(vencidas);
        }
    }
}
