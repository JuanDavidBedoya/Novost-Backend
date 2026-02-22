package com.restaurante.backend.components;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.repositories.ReservaRepository;
import com.restaurante.backend.services.EmailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReservaTask {

    private final ReservaRepository reservaRepo;

    private final EmailService emailService;

    // Recordatorio 12 horas antes
    @Scheduled(cron = "0 0 * * * *") // Se ejecuta cada hora al minuto 0
    public void enviarRecordatorios12Horas() {
        // Calculamos el momento exacto de aquí a 12 horas
        LocalDateTime ahoraMas12 = LocalDateTime.now().plusHours(12);
        LocalDate fechaBusqueda = ahoraMas12.toLocalDate();
        
        // Definimos una ventana de 1 hora para capturar las reservas
        LocalTime inicioVentana = ahoraMas12.toLocalTime().withMinute(0).withSecond(0);
        LocalTime finVentana = inicioVentana.plusHours(1);

        List<Reserva> proximas = reservaRepo.findReservasParaRecordatorio(fechaBusqueda, inicioVentana, finVentana);

        proximas.forEach(r -> {
            emailService.enviarCorreo(
                r.getUsuario().getEmail(),
                "Recordatorio de tu reserva - Novost",
                "Hola " + r.getUsuario().getNombre() + ", te recordamos tu reserva para hoy a las " + r.getHoraInicio() + ". ¡Te esperamos!"
            );
        });
    }

    // Se ejecuta cada hora (cron: segundos minutos horas día mes día-semana)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cancelarReservasNoPagadas() {
        LocalDate limite = LocalDate.now().plusDays(1);
        List<Reserva> vencidas = reservaRepo.findReservasNoPagadasVencidas(limite);

        vencidas.forEach(r -> {
            r.getEstadoReserva().setNombre("CANCELADA_POR_IMPAGO");
            emailService.enviarCorreo(r.getUsuario().getEmail(), 
                "Reserva Cancelada - Novost", 
                "Su reserva para el día " + r.getFecha() + " ha sido cancelada por falta de pago.");
        });
        reservaRepo.saveAll(vencidas);
    }
}
