package com.restaurante.backend.services;

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

    public void enviarCorreo(String destinado, String asunto, String cuerpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinado);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
    }

    public void enviarFactura(Pago pago) {
        Reserva r = pago.getReserva();
        String cuerpo = String.format(
            "FACTURA DE PAGO - Novost\n\n" +
            "Cliente: %s\n" +
            "Empresa: Novost SAS\n" +
            "Fecha Reserva: %s\n" +
            "Horario: %s a %s\n" +
            "Monto Pagado: $%s\n" +
            "ID Transacción: %s\n\n" +
            "¡Gracias por su preferencia!",
            r.getUsuario().getNombre(),
            r.getFecha(), r.getHoraInicio(), r.getHoraFin(),
            pago.getMonto(), pago.getIdPasarela()
        );
        enviarCorreo(r.getUsuario().getEmail(), "Su Factura de Pago - Novost", cuerpo);
    }
}
