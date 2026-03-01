package com.restaurante.backend.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.restaurante.backend.entities.Pago;

import jakarta.mail.internet.MimeMessage;
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

    /**
     * Envía la factura detallada tras un pago exitoso.
     * Se usa la entidad Pago porque ya contiene la relación con Reserva y Usuario.
     */
    public void enviarFacturaConPDF(Pago pago) {
        try {
            // 1. Crear el mensaje MIME (permite adjuntos)
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true = multipart

            helper.setTo(pago.getReserva().getUsuario().getEmail());
            helper.setSubject("Tu Factura de Reserva #" + pago.getReserva().getIdReserva());
            helper.setText("Hola " + pago.getReserva().getUsuario().getNombre() + 
                           ", adjuntamos el detalle de tu pago.");

            // 2. Generar el PDF en un array de bytes
            byte[] pdfBytes = generarPDF(pago);

            // 3. Adjuntar el PDF al correo
            helper.addAttachment("Factura_" + pago.getReserva().getIdReserva() + ".pdf", 
                                 new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("📧 PDF enviado con éxito.");

        } catch (Exception e) {
            System.err.println("❌ Fallo al enviar PDF: " + e.getMessage());
        }
    }

    public void enviarRecordatorio(String email, String nombre, LocalDate fecha, LocalTime hora) {
        String cuerpo = String.format(
            "Hola %s,\n\nTe recordamos que tienes una reserva hoy %s a las %s en Novost.\n" +
            "¡Estamos preparando todo para tu llegada!",
            nombre, fecha, hora
        );
        enviarCorreo(email, "Recordatorio de tu Reserva - Novost", cuerpo);
    }

    public void enviarNotificacionCancelacion(String email, String nombre, String motivo) {
        String cuerpo = String.format(
            "Hola %s,\n\nTe informamos que tu reserva en Novost ha sido cancelada.\n" +
            "Motivo: %s\n\nSi tienes dudas, por favor contáctanos.",
            nombre, motivo
        );
        enviarCorreo(email, "Actualización de Reserva - Novost", cuerpo);
    }

    private byte[] generarPDF(Pago pago) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();

        // 1. Encabezado de la Factura
        Font boldFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        document.add(new Paragraph("FACTURA DE RESERVA - NOVOST", boldFont));
        document.add(new Paragraph("ID Transacción: " + pago.getIdPasarela()));
        document.add(new Paragraph("Fecha: " + pago.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        document.add(new Paragraph("------------------------------------------------------------------"));

        // 2. Información del Cliente
        document.add(new Paragraph("Cliente: " + pago.getReserva().getUsuario().getNombre()));
        document.add(new Paragraph("Cédula: " + pago.getReserva().getUsuario().getCedula()));
        document.add(new Paragraph("Mesa: #" + pago.getReserva().getMesa().getNumeroMesa()));
        document.add(new Paragraph("------------------------------------------------------------------"));

        // 3. DESGLOSE DEL MONTO (Aquí aplicamos la lógica de los 5 USD)
        int personas = pago.getReserva().getNumPersonas();
        double precioPersona = 5.0; // Tu valor base
        double total = pago.getMonto(); // El monto que guardamos en la DB

        document.add(new Paragraph("CONCEPTO: Reserva de mesa"));
        document.add(new Paragraph("Cantidad de personas: " + personas));
        document.add(new Paragraph("Precio por persona: $ " + precioPersona + " USD"));
        document.add(new Paragraph(" ")); // Espacio en blanco
        
        // 4. Total resaltado
        Font totalFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        document.add(new Paragraph("TOTAL PAGADO: $ " + total + " USD", totalFont));
        
        document.add(new Paragraph("------------------------------------------------------------------"));
        document.add(new Paragraph("¡Gracias por elegir Novost! Esperamos verle pronto."));

        document.close();
        return out.toByteArray();
    }
}
