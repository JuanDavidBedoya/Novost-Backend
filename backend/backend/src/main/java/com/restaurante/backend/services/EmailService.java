package com.restaurante.backend.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.PagoPedido;
import com.restaurante.backend.entities.Pedido;
import com.restaurante.backend.entities.PedidoDetalle;
import com.restaurante.backend.repositories.PedidoDetalleRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final PedidoDetalleRepository pedidoDetalleRepo;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Utilidades generales ──────────────────────────────────────────────────

    public void enviarCorreo(String destinado, String asunto, String cuerpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinado);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
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

    // ── Factura de RESERVA ────────────────────────────────────────────────────

    public void enviarFacturaConPDF(Pago pago) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(pago.getReserva().getUsuario().getEmail());
            helper.setSubject("Tu Factura de Reserva #" + pago.getReserva().getIdReserva());
            helper.setText("Hola " + pago.getReserva().getUsuario().getNombre() +
                           ", adjuntamos el detalle de tu pago.");

            byte[] pdfBytes = generarPDF(pago);
            helper.addAttachment(
                "Factura_" + pago.getReserva().getIdReserva() + ".pdf",
                new ByteArrayResource(pdfBytes)
            );

            mailSender.send(message);
            System.out.println("PDF de reserva enviado con éxito.");

        } catch (Exception e) {
            System.err.println("Fallo al enviar PDF de reserva: " + e.getMessage());
        }
    }

    private byte[] generarPDF(Pago pago) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font boldFont   = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
        Font totalFont  = new Font(Font.HELVETICA, 14, Font.BOLD);

        document.add(new Paragraph("FACTURA DE RESERVA - NOVOST", boldFont));
        document.add(new Paragraph("ID Transacción: " + pago.getIdPasarela(), normalFont));
        document.add(new Paragraph("Fecha: " + pago.getFechaPago().format(FORMATO_FECHA), normalFont));
        document.add(new Paragraph("------------------------------------------------------------------"));
        document.add(new Paragraph("Cliente: " + pago.getReserva().getUsuario().getNombre(), normalFont));
        document.add(new Paragraph("Cédula: "  + pago.getReserva().getUsuario().getCedula(), normalFont));
        document.add(new Paragraph("Mesa: #"   + pago.getReserva().getMesa().getNumeroMesa(), normalFont));
        document.add(new Paragraph("------------------------------------------------------------------"));

        int personas       = pago.getReserva().getNumPersonas();
        double precioPersona = 5.0;
        double total       = pago.getMonto();

        document.add(new Paragraph("CONCEPTO: Reserva de mesa", normalFont));
        document.add(new Paragraph("Cantidad de personas: " + personas, normalFont));
        document.add(new Paragraph("Precio por persona: $ " + precioPersona + " USD", normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("TOTAL PAGADO: $ " + total + " USD", totalFont));
        document.add(new Paragraph("------------------------------------------------------------------"));
        document.add(new Paragraph("¡Gracias por elegir Novost! Esperamos verle pronto.", normalFont));

        document.close();
        return out.toByteArray();
    }

    // ── Factura de PEDIDO ─────────────────────────────────────────────────────

    public void enviarFacturaPedido(PagoPedido pagoPedido, String emailDestino, String nombreCliente) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(emailDestino);
            helper.setSubject("Tu Factura de Pedido #" + pagoPedido.getPedido().getIdPedido() + " - Novost");
            helper.setText("Hola " + nombreCliente + ", adjuntamos el detalle de tu pedido en Novost.");

            byte[] pdfBytes = generarPDFPedido(pagoPedido);
            helper.addAttachment(
                "Factura_Pedido_" + pagoPedido.getPedido().getIdPedido() + ".pdf",
                new ByteArrayResource(pdfBytes)
            );

            mailSender.send(message);
            System.out.println("Factura de pedido enviada con éxito.");

        } catch (Exception e) {
            System.err.println("Fallo al enviar factura de pedido: " + e.getMessage());
        }
    }

    private byte[] generarPDFPedido(PagoPedido pagoPedido) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font tituloFont  = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font seccionFont = new Font(Font.HELVETICA, 13, Font.BOLD);
        Font normalFont  = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font totalFont   = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font headerTabla = new Font(Font.HELVETICA, 11, Font.BOLD);

        Pedido pedido = pagoPedido.getPedido();

        // ── Encabezado ────────────────────────────────────────────────────────
        document.add(new Paragraph("FACTURA DE PEDIDO - NOVOST", tituloFont));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Pedido N°: #" + pedido.getIdPedido(), normalFont));
        document.add(new Paragraph("Fecha:     " +
                pagoPedido.getFechaPago().format(FORMATO_FECHA), normalFont));
        document.add(new Paragraph("Mesa:      #" + pedido.getMesa().getNumeroMesa() +
                " (" + pedido.getMesa().getCapacidad() + " personas)", normalFont));

        String idTx = pagoPedido.getIdPasarela() != null
                ? pagoPedido.getIdPasarela() : "Pago en Caja";
        document.add(new Paragraph("Transacción: " + idTx, normalFont));
        document.add(new Paragraph("Método de pago: " + pagoPedido.getMetodoPago(), normalFont));

        if (pedido.getReserva() != null) {
            document.add(new Paragraph("Reserva asociada: #" +
                    pedido.getReserva().getIdReserva(), normalFont));
        }

        document.add(new Paragraph("------------------------------------------------------------------"));

        // ── Detalle de platos ─────────────────────────────────────────────────
        document.add(new Paragraph("DETALLE DEL PEDIDO", seccionFont));
        document.add(new Paragraph(" "));

        // Tabla: Plato | Cant. | Precio unit. | Subtotal
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{4f, 1.2f, 2f, 2f});

        // Cabecera de la tabla
        String[] cabeceras = { "Plato", "Cant.", "Precio unit.", "Subtotal" };
        for (String cab : cabeceras) {
            PdfPCell celda = new PdfPCell(new Phrase(cab, headerTabla));
            celda.setBackgroundColor(new java.awt.Color(230, 220, 255));
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(6);
            tabla.addCell(celda);
        }

        // Filas con los platos
        List<PedidoDetalle> detalles = pedidoDetalleRepo.findByPedido(pedido);
        for (PedidoDetalle detalle : detalles) {
            agregarCeldaTabla(tabla, detalle.getPlato().getNombrePlato(), normalFont, Element.ALIGN_LEFT);
            agregarCeldaTabla(tabla, String.valueOf(detalle.getCantidad()),           normalFont, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, "$ " + String.format("%.2f", detalle.getPrecioUnitario()), normalFont, Element.ALIGN_RIGHT);
            agregarCeldaTabla(tabla, "$ " + String.format("%.2f", detalle.getSubtotal()),        normalFont, Element.ALIGN_RIGHT);
        }

        document.add(tabla);
        document.add(new Paragraph(" "));

        // ── Observaciones ─────────────────────────────────────────────────────
        if (pedido.getObservaciones() != null && !pedido.getObservaciones().isBlank()) {
            document.add(new Paragraph("Observaciones: " + pedido.getObservaciones(), normalFont));
            document.add(new Paragraph(" "));
        }

        // ── Totales ───────────────────────────────────────────────────────────
        document.add(new Paragraph("------------------------------------------------------------------"));
        document.add(new Paragraph("Subtotal:  $ " +
                String.format("%.2f", pedido.getSubtotal()) + " USD", normalFont));
        document.add(new Paragraph("IVA (19%): $ " +
                String.format("%.2f", pedido.getTotal() - pedido.getSubtotal()) + " USD", normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("TOTAL PAGADO: $ " +
                String.format("%.2f", pedido.getTotal()) + " USD", totalFont));
        document.add(new Paragraph("------------------------------------------------------------------"));
        document.add(new Paragraph("¡Gracias por elegir Novost! Esperamos verte pronto.", normalFont));

        document.close();
        return out.toByteArray();
    }

    // ── Helper para celdas de tabla ───────────────────────────────────────────

    private void agregarCeldaTabla(PdfPTable tabla, String texto, Font font, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5);
        tabla.addCell(celda);
    }
}