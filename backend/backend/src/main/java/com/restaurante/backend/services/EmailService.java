package com.restaurante.backend.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Método genérico para enviar correos de texto plano.
     */
    public void enviarCorreo(String destinado, String asunto, String cuerpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinado);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
    }

    /**
     * Envía la factura detallada tras un pago exitoso.
     * Se usa la entidad Pago porque ya contiene la relación con Reserva y Usuario.
     */
    public void enviarFactura(Pago pago) {
        Reserva r = pago.getReserva();
        String cuerpo = String.format(
            "--- FACTURA DE PAGO - NOVOST ---\n\n" +
            "Cliente: %s\n" +
            "Empresa: Novost SAS\n" +
            "Fecha de Reserva: %s\n" +
            "Horario: %s a %s\n" +
            "---------------------------------\n" +
            "Monto Pagado: $%.2f\n" +
            "ID Transacción Pasarela: %s\n" +
            "Fecha de Pago: %s\n" +
            "---------------------------------\n" +
            "¡Gracias por elegir Novost! Te esperamos.",
            r.getUsuario().getNombre(),
            r.getFecha(), 
            r.getHoraInicio(), 
            r.getHoraFin(),
            pago.getMonto(), 
            pago.getIdPasarela(),
            pago.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
        enviarCorreo(r.getUsuario().getEmail(), "Confirmación de Pago - Novost", cuerpo);
    }

    /**
     * Envía un recordatorio automático antes de la reserva.
     */
    public void enviarRecordatorio(String email, String nombre, LocalDate fecha, LocalTime hora) {
        String cuerpo = String.format(
            "Hola %s,\n\nTe recordamos que tienes una reserva hoy %s a las %s en Novost.\n" +
            "¡Estamos preparando todo para tu llegada!",
            nombre, fecha, hora
        );
        enviarCorreo(email, "Recordatorio de tu Reserva - Novost", cuerpo);
    }

    /**
     * Notifica sobre la cancelación (Manual o Automática).
     */
    public void enviarNotificacionCancelacion(String email, String nombre, String motivo) {
        String cuerpo = String.format(
            "Hola %s,\n\nTe informamos que tu reserva en Novost ha sido cancelada.\n" +
            "Motivo: %s\n\nSi tienes dudas, por favor contáctanos.",
            nombre, motivo
        );
        enviarCorreo(email, "Actualización de Reserva - Novost", cuerpo);
    }
}
