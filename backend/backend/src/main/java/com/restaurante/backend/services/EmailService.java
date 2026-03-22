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
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
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

    // RECORDATORIOS

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

    // ── Header HTML compartido ────────────────────────────────────────────────

    private String buildHeader(String subtitulo) {
        return
            "<!DOCTYPE html>" +
            "<html lang='es'>" +
            "<head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
            "<body style='margin:0;padding:0;background:#f4f4f8;" +
            "font-family:\"Segoe UI\",Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' " +
            "style='background:#f4f4f8;padding:40px 0;'>" +
            "<tr><td align='center'>" +
            "<table width='520' cellpadding='0' cellspacing='0' style='" +
            "background:#ffffff;border-radius:24px;" +
            "box-shadow:0 8px 32px rgba(126,34,206,0.10);" +
            "overflow:hidden;max-width:520px;width:100%;'>" +
            // ── Header morado ──
            "<tr><td style='" +
            "background:linear-gradient(135deg,#7E22CE 0%,#a855f7 100%);" +
            "padding:36px 40px 28px;text-align:center;'>" +
            "<p style='margin:0 0 10px;font-size:32px;font-weight:900;color:#ffffff;" +
            "letter-spacing:4px;font-family:\"Segoe UI\",Arial,sans-serif;'>NOVOST</p>" +
            "<p style='margin:0;font-size:13px;color:rgba(255,255,255,0.8);" +
            "font-weight:500;'>" + subtitulo + "</p>" +
            "</td></tr>";
    }

    // ── Footer HTML compartido ────────────────────────────────────────────────

    private static final String FOOTER =
        "<tr><td style='background:#f9fafb;border-top:1px solid #f0f0f5;" +
        "padding:24px 40px;text-align:center;'>" +
        "<p style='margin:0;font-size:12px;color:#9ca3af;line-height:1.6;'>" +
        "Este correo fue enviado automáticamente por " +
        "<strong style='color:#7E22CE;'>Novost</strong>.<br>" +
        "Por favor no respondas a este mensaje." +
        "</p></td></tr>" +
        "</table></td></tr></table>" +
        "</body></html>";

    // ── Código de verificación 2FA (HTML) ────────────────────────────────────

    public void enviarCodigoVerificacion(String email, String nombre, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart/related para permitir imágenes inline CID
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Tu código de acceso a Novost");
            helper.setText(buildCodigoHtml(nombre, codigo), true);
            mailSender.send(message);
            System.out.println("Código de verificación enviado a: " + email);
        } catch (Exception e) {
            System.err.println("Fallo al enviar código de verificación: " + e.getMessage());
        }
    }

    private String buildCodigoHtml(String nombre, String codigo) {
        String[] digitos = codigo.split("");
        StringBuilder cajasDigitos = new StringBuilder();
        for (String d : digitos) {
            cajasDigitos.append(
                "<td style='width:48px;height:56px;background:#f5f3ff;" +
                "border:2px solid #e9d5ff;border-radius:12px;" +
                "text-align:center;vertical-align:middle;" +
                "font-size:28px;font-weight:900;color:#7E22CE;" +
                "font-family:\"Segoe UI\",Arial,sans-serif;padding:0 4px;'>" +
                d + "</td><td style='width:8px;'></td>"
            );
        }

        return buildHeader("Verificación de identidad") +
            "<tr><td style='padding:40px 40px 32px;'>" +
            "<p style='margin:0 0 8px;font-size:22px;font-weight:800;color:#1a1a2e;'>" +
            "Hola, " + nombre + " 👋</p>" +
            "<p style='margin:0 0 32px;font-size:15px;color:#6b7280;line-height:1.6;'>" +
            "Ingresa el siguiente código para completar tu inicio de sesión. " +
            "Este código <strong style='color:#1a1a2e;'>expira en 2 minutos</strong>." +
            "</p>" +
            "<table cellpadding='0' cellspacing='0' style='margin:0 auto 32px;'>" +
            "<tr>" + cajasDigitos + "</tr></table>" +
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td style='background:#fdf4ff;border:1.5px solid #e9d5ff;" +
            "border-radius:12px;padding:16px 20px;'>" +
            "<p style='margin:0;font-size:13px;color:#7E22CE;font-weight:600;line-height:1.6;'>" +
            "🔒 <strong>¿No fuiste tú?</strong> Si no solicitaste este código, " +
            "ignora este correo. Tu cuenta sigue segura." +
            "</p></td></tr></table>" +
            "</td></tr>" +
            FOOTER;
    }

    // ── Recuperación de contraseña (HTML) ─────────────────────────────────────

    public void enviarRecuperacionPassword(String email, String nombre, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Recupera tu contraseña - Novost");
            helper.setText(buildRecuperacionHtml(nombre, link), true);
            mailSender.send(message);
            System.out.println("Correo de recuperación enviado a: " + email);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de recuperación: " + e.getMessage());
        }
    }

    private String buildRecuperacionHtml(String nombre, String link) {
        return buildHeader("Recuperación de contraseña") +
            "<tr><td style='padding:40px 40px 32px;'>" +
            "<div style='text-align:center;margin-bottom:24px;'>" +
            "<div style='display:inline-block;background:#f5f3ff;border-radius:50%;" +
            "width:64px;height:64px;line-height:64px;text-align:center;font-size:32px;'>" +
            "🔑</div></div>" +
            "<p style='margin:0 0 8px;font-size:22px;font-weight:800;color:#1a1a2e;" +
            "text-align:center;'>Hola, " + nombre + "</p>" +
            "<p style='margin:0 0 32px;font-size:15px;color:#6b7280;line-height:1.6;" +
            "text-align:center;'>" +
            "Recibimos una solicitud para restablecer la contraseña de tu cuenta. " +
            "Haz clic en el botón para continuar.<br>" +
            "<strong style='color:#1a1a2e;'>Este enlace expira en 5 minutos.</strong>" +
            "</p>" +
            "<table cellpadding='0' cellspacing='0' style='margin:0 auto 32px;'><tr>" +
            "<td style='background:linear-gradient(135deg,#7E22CE 0%,#a855f7 100%);" +
            "border-radius:14px;'>" +
            "<a href='" + link + "' target='_blank' style='" +
            "display:inline-block;padding:16px 40px;" +
            "font-size:16px;font-weight:800;color:#ffffff;text-decoration:none;" +
            "letter-spacing:0.5px;border-radius:14px;'>Restablecer contraseña →</a>" +
            "</td></tr></table>" +
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td style='background:#f9fafb;border:1.5px solid #e5e7eb;" +
            "border-radius:12px;padding:14px 18px;'>" +
            "<p style='margin:0 0 6px;font-size:12px;color:#9ca3af;font-weight:600;" +
            "text-transform:uppercase;letter-spacing:0.5px;'>¿El botón no funciona?</p>" +
            "<p style='margin:0;font-size:12px;color:#6b7280;word-break:break-all;'>" +
            "Copia y pega este enlace en tu navegador:<br>" +
            "<span style='color:#7E22CE;font-weight:600;'>" + link + "</span>" +
            "</p></td></tr></table>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-top:20px;'>" +
            "<tr><td style='background:#fff8e1;border:1.5px solid #fde68a;" +
            "border-radius:12px;padding:14px 18px;'>" +
            "<p style='margin:0;font-size:13px;color:#b45309;font-weight:600;line-height:1.6;'>" +
            "⚠️ <strong>¿No solicitaste esto?</strong> Ignora este correo. " +
            "Tu contraseña no cambiará hasta que uses el enlace." +
            "</p></td></tr></table>" +
            "</td></tr>" +
            FOOTER;
    }

    // ── Bienvenida usuario nuevo (HTML) ──────────────────────────────────────

    public void enviarBienvenidaUsuario(String email, String nombre) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("¡Bienvenido a Novost, " + nombre + "!");
            helper.setText(buildBienvenidaUsuarioHtml(nombre), true);
            mailSender.send(message);
            System.out.println("Correo de bienvenida enviado a: " + email);
        } catch (Exception e) {
            System.err.println("Fallo al enviar bienvenida de usuario: " + e.getMessage());
        }
    }

    private String buildBienvenidaUsuarioHtml(String nombre) {
        return buildHeader("¡Bienvenido a Novost!") +
            "<tr><td style='padding:40px 40px 32px;'>" +

            // Ícono celebración
            "<div style='text-align:center;margin-bottom:24px;'>" +
            "<div style='display:inline-block;background:#f5f3ff;border-radius:50%;" +
            "width:64px;height:64px;line-height:64px;text-align:center;font-size:32px;'>" +
            "🎉</div></div>" +

            // Saludo
            "<p style='margin:0 0 8px;font-size:22px;font-weight:800;color:#1a1a2e;" +
            "text-align:center;'>¡Hola, " + nombre + "!</p>" +
            "<p style='margin:0 0 28px;font-size:15px;color:#6b7280;line-height:1.6;" +
            "text-align:center;'>" +
            "Tu registro en Novost ha sido exitoso. " +
            "Ahora puedes hacer reservas, ver el menú y gestionar tus pedidos." +
            "</p>" +

            // Tarjetas de características
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:16px;'>" +
            "<tr>" +
            // Reservas
            "<td style='width:50%;padding-right:8px;vertical-align:top;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td style='background:#f5f3ff;border-radius:12px;padding:16px;text-align:center;'>" +
            "<div style='font-size:24px;margin-bottom:6px;'>📅</div>" +
            "<p style='margin:0;font-size:13px;font-weight:700;color:#7E22CE;'>Reservas</p>" +
            "<p style='margin:4px 0 0;font-size:12px;color:#6b7280;'>Reserva tu mesa fácilmente</p>" +
            "</td></tr></table></td>" +
            // Pedidos
            "<td style='width:50%;padding-left:8px;vertical-align:top;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td style='background:#f0fdf4;border-radius:12px;padding:16px;text-align:center;'>" +
            "<div style='font-size:24px;margin-bottom:6px;'>🍽️</div>" +
            "<p style='margin:0;font-size:13px;font-weight:700;color:#16a34a;'>Pedidos</p>" +
            "<p style='margin:4px 0 0;font-size:12px;color:#6b7280;'>Pide desde la mesa</p>" +
            "</td></tr></table></td>" +
            "</tr></table>" +

            // Aviso final
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-top:8px;'><tr>" +
            "<td style='background:#fdf4ff;border:1.5px solid #e9d5ff;" +
            "border-radius:12px;padding:16px 20px;'>" +
            "<p style='margin:0;font-size:13px;color:#7E22CE;font-weight:600;line-height:1.6;" +
            "text-align:center;'>" +
            "✨ ¡Estamos felices de tenerte con nosotros! Esperamos verte pronto en Novost." +
            "</p></td></tr></table>" +

            "</td></tr>" +
            FOOTER;
    }

    // ── Bienvenida trabajador nuevo (HTML) ────────────────────────────────────

    public void enviarBienvenidaTrabajador(String email, String nombre, String contrasena) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Bienvenido al equipo Novost, " + nombre);
            helper.setText(buildBienvenidaTrabajadorHtml(nombre, contrasena), true);
            mailSender.send(message);
            System.out.println("Correo de bienvenida trabajador enviado a: " + email);
        } catch (Exception e) {
            System.err.println("Fallo al enviar bienvenida de trabajador: " + e.getMessage());
        }
    }

    private String buildBienvenidaTrabajadorHtml(String nombre, String contrasena) {
        return buildHeader("Acceso al sistema") +
            "<tr><td style='padding:40px 40px 32px;'>" +

            // Ícono
            "<div style='text-align:center;margin-bottom:24px;'>" +
            "<div style='display:inline-block;background:#f5f3ff;border-radius:50%;" +
            "width:64px;height:64px;line-height:64px;text-align:center;font-size:32px;'>" +
            "👨‍🍳</div></div>" +

            // Saludo
            "<p style='margin:0 0 8px;font-size:22px;font-weight:800;color:#1a1a2e;" +
            "text-align:center;'>¡Hola, " + nombre + "!</p>" +
            "<p style='margin:0 0 28px;font-size:15px;color:#6b7280;line-height:1.6;" +
            "text-align:center;'>" +
            "Has sido registrado como trabajador en el sistema de Novost. " +
            "A continuación encontrarás tus credenciales de acceso." +
            "</p>" +

            // Caja credenciales
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'><tr>" +
            "<td style='background:#f8f9fa;border:1.5px solid #e5e7eb;" +
            "border-radius:14px;padding:20px 24px;'>" +
            "<p style='margin:0 0 4px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>Correo electrónico</p>" +
            "<p style='margin:0 0 16px;font-size:15px;font-weight:600;color:#1a1a2e;'>" +
            "Tu correo registrado</p>" +
            "<p style='margin:0 0 4px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>Contraseña temporal</p>" +
            "<p style='margin:0;font-size:20px;font-weight:900;color:#7E22CE;" +
            "letter-spacing:2px;background:#f5f3ff;border-radius:8px;" +
            "padding:8px 14px;display:inline-block;'>" + contrasena + "</p>" +
            "</td></tr></table>" +

            // Aviso cambio contraseña
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:16px;'><tr>" +
            "<td style='background:#fff8e1;border:1.5px solid #fde68a;" +
            "border-radius:12px;padding:14px 18px;'>" +
            "<p style='margin:0;font-size:13px;color:#b45309;font-weight:600;line-height:1.6;'>" +
            "⚠️ <strong>Por tu seguridad</strong>, cambia esta contraseña la primera vez que ingreses al sistema." +
            "</p></td></tr></table>" +

            // Aviso acceso
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td style='background:#fdf4ff;border:1.5px solid #e9d5ff;" +
            "border-radius:12px;padding:14px 18px;'>" +
            "<p style='margin:0;font-size:13px;color:#7E22CE;font-weight:600;line-height:1.6;" +
            "text-align:center;'>" +
            "🍽️ ¡Bienvenido al equipo Novost! Estamos felices de contarte entre nosotros." +
            "</p></td></tr></table>" +

            "</td></tr>" +
            FOOTER;
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
            helper.addAttachment("Factura_" + pago.getReserva().getIdReserva() + ".pdf",
                new ByteArrayResource(pdfBytes));
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

        int    personas      = pago.getReserva().getNumPersonas();
        double precioPersona = 5.0;
        double total         = pago.getMonto();

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

    // ── Factura de PEDIDO (HTML) ──────────────────────────────────────────────

    public void enviarFacturaPedido(PagoPedido pagoPedido, String emailDestino, String nombreCliente) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailDestino);
            helper.setSubject("Tu Factura de Pedido #" +
                    pagoPedido.getPedido().getIdPedido() + " - Novost");
            helper.setText(buildFacturaPedidoHtml(pagoPedido, nombreCliente), true);
            mailSender.send(message);
            System.out.println("Factura de pedido enviada con éxito.");
        } catch (Exception e) {
            System.err.println("Fallo al enviar factura de pedido: " + e.getMessage());
        }
    }

    private String buildFacturaPedidoHtml(PagoPedido pagoPedido, String nombreCliente) {
        Pedido pedido = pagoPedido.getPedido();
        List<PedidoDetalle> detalles = pedidoDetalleRepo.findByPedido(pedido);

        String idTx = pagoPedido.getIdPasarela() != null
                ? pagoPedido.getIdPasarela() : "Pago en Caja";

        // ── Filas de la tabla de platos ───────────────────────────────────────
        StringBuilder filasPlatos = new StringBuilder();
        for (PedidoDetalle detalle : detalles) {
            filasPlatos.append(
                "<tr>" +
                "<td style='padding:10px 12px;font-size:14px;color:#1a1a2e;" +
                "border-bottom:1px solid #f0f0f5;'>" +
                detalle.getPlato().getNombrePlato() + "</td>" +
                "<td style='padding:10px 12px;font-size:14px;color:#374151;" +
                "text-align:center;border-bottom:1px solid #f0f0f5;'>" +
                detalle.getCantidad() + "</td>" +
                "<td style='padding:10px 12px;font-size:14px;color:#374151;" +
                "text-align:right;border-bottom:1px solid #f0f0f5;'>" +
                "$ " + String.format("%.2f", detalle.getPrecioUnitario()) + "</td>" +
                "<td style='padding:10px 12px;font-size:14px;font-weight:700;color:#7E22CE;" +
                "text-align:right;border-bottom:1px solid #f0f0f5;'>" +
                "$ " + String.format("%.2f", detalle.getSubtotal()) + "</td>" +
                "</tr>"
            );
        }

        // ── Fila de observaciones (solo si existe) ────────────────────────────
        String observacionesHtml = "";
        if (pedido.getObservaciones() != null && !pedido.getObservaciones().isBlank()) {
            observacionesHtml =
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:20px;'><tr>" +
                "<td style='background:#f9fafb;border:1.5px solid #e5e7eb;" +
                "border-radius:12px;padding:14px 18px;'>" +
                "<p style='margin:0 0 4px;font-size:11px;font-weight:700;color:#9ca3af;" +
                "text-transform:uppercase;letter-spacing:0.8px;'>Observaciones</p>" +
                "<p style='margin:0;font-size:14px;color:#374151;font-style:italic;'>" +
                pedido.getObservaciones() + "</p>" +
                "</td></tr></table>";
        }

        return buildHeader("Factura de Pedido") +
            "<tr><td style='padding:40px 40px 32px;'>" +

            // Saludo
            "<p style='margin:0 0 6px;font-size:22px;font-weight:800;color:#1a1a2e;'>" +
            "¡Gracias, " + nombreCliente + "! 🍽️</p>" +
            "<p style='margin:0 0 28px;font-size:15px;color:#6b7280;line-height:1.6;'>" +
            "Aquí tienes el detalle de tu pedido en Novost." +
            "</p>" +

            // Info del pedido
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'><tr>" +
            "<td style='background:#f8f9fa;border:1.5px solid #e5e7eb;" +
            "border-radius:14px;padding:18px 22px;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'>" +
            // Fila 1: Pedido + Mesa
            "<tr>" +
            "<td style='width:50%;padding-bottom:12px;'>" +
            "<p style='margin:0 0 3px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>Pedido N°</p>" +
            "<p style='margin:0;font-size:18px;font-weight:900;color:#7E22CE;'>" +
            "#" + pedido.getIdPedido() + "</p></td>" +
            "<td style='width:50%;padding-bottom:12px;'>" +
            "<p style='margin:0 0 3px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>Mesa</p>" +
            "<p style='margin:0;font-size:16px;font-weight:700;color:#1a1a2e;'>" +
            "#" + pedido.getMesa().getNumeroMesa() +
            " <span style='font-size:13px;color:#9ca3af;font-weight:500;'>(" +
            pedido.getMesa().getCapacidad() + " personas)</span></p></td>" +
            "</tr>" +
            // Fila 2: Fecha + Método de pago
            "<tr>" +
            "<td style='padding-bottom:12px;'>" +
            "<p style='margin:0 0 3px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>Fecha</p>" +
            "<p style='margin:0;font-size:14px;font-weight:600;color:#374151;'>" +
            pagoPedido.getFechaPago().format(FORMATO_FECHA) + "</p></td>" +
            "<td style='padding-bottom:12px;'>" +
            "<p style='margin:0 0 3px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>Método de pago</p>" +
            "<p style='margin:0;font-size:14px;font-weight:600;color:#374151;'>" +
            pagoPedido.getMetodoPago() + "</p></td>" +
            "</tr>" +
            // Fila 3: Transacción (solo si hay pasarela)
            (pagoPedido.getIdPasarela() != null ?
            "<tr><td colspan='2'>" +
            "<p style='margin:0 0 3px;font-size:11px;font-weight:700;color:#9ca3af;" +
            "text-transform:uppercase;letter-spacing:0.8px;'>ID Transacción</p>" +
            "<p style='margin:0;font-size:12px;font-weight:600;color:#374151;" +
            "word-break:break-all;'>" + idTx + "</p></td></tr>" : "") +
            "</table>" +
            "</td></tr></table>" +

            // Tabla de platos
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;" +
            "border-radius:14px;overflow:hidden;border:1.5px solid #e5e7eb;'>" +
            // Cabecera tabla
            "<thead><tr style='background:linear-gradient(135deg,#7E22CE 0%,#a855f7 100%);'>" +
            "<th style='padding:12px;font-size:11px;font-weight:800;color:#ffffff;" +
            "text-transform:uppercase;letter-spacing:0.8px;text-align:left;'>Plato</th>" +
            "<th style='padding:12px;font-size:11px;font-weight:800;color:#ffffff;" +
            "text-transform:uppercase;letter-spacing:0.8px;text-align:center;'>Cant.</th>" +
            "<th style='padding:12px;font-size:11px;font-weight:800;color:#ffffff;" +
            "text-transform:uppercase;letter-spacing:0.8px;text-align:right;'>Precio unit.</th>" +
            "<th style='padding:12px;font-size:11px;font-weight:800;color:#ffffff;" +
            "text-transform:uppercase;letter-spacing:0.8px;text-align:right;'>Subtotal</th>" +
            "</tr></thead>" +
            "<tbody>" + filasPlatos + "</tbody>" +
            "</table>" +

            // Observaciones
            observacionesHtml +

            // Totales
            "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'><tr>" +
            "<td style='background:#f8f9fa;border:1.5px solid #e5e7eb;" +
            "border-radius:14px;padding:18px 22px;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'>" +
            "<tr><td style='padding-bottom:8px;'>" +
            "<span style='font-size:14px;color:#6b7280;'>Subtotal</span></td>" +
            "<td style='padding-bottom:8px;text-align:right;'>" +
            "<span style='font-size:14px;font-weight:600;color:#374151;'>" +
            "$ " + String.format("%.2f", pedido.getSubtotal()) + " USD</span></td></tr>" +
            "<tr><td style='padding-bottom:12px;'>" +
            "<span style='font-size:14px;color:#6b7280;'>IVA (19%)</span></td>" +
            "<td style='padding-bottom:12px;text-align:right;'>" +
            "<span style='font-size:14px;font-weight:600;color:#374151;'>" +
            "$ " + String.format("%.2f", pedido.getTotal() - pedido.getSubtotal()) + " USD</span></td></tr>" +
            "<tr style='border-top:2px dashed #e5e7eb;'>" +
            "<td style='padding-top:12px;'>" +
            "<span style='font-size:16px;font-weight:900;color:#1a1a2e;'>Total pagado</span></td>" +
            "<td style='padding-top:12px;text-align:right;'>" +
            "<span style='font-size:20px;font-weight:900;color:#7E22CE;'>" +
            "$ " + String.format("%.2f", pedido.getTotal()) + " USD</span></td></tr>" +
            "</table>" +
            "</td></tr></table>" +

            // Mensaje final
            "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "<td style='background:#fdf4ff;border:1.5px solid #e9d5ff;" +
            "border-radius:12px;padding:16px 20px;'>" +
            "<p style='margin:0;font-size:13px;color:#7E22CE;font-weight:600;" +
            "line-height:1.6;text-align:center;'>" +
            "✨ ¡Gracias por elegir Novost! Esperamos verte pronto." +
            "</p></td></tr></table>" +

            "</td></tr>" +
            FOOTER;
    }

}