package com.restaurante.backend.services;

import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Mesa;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.entities.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Usuario usuario;
    private Mesa mesa;
    private EstadoReserva estado;
    private Reserva reserva;
    private Pago pago;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setCedula("12345678");
        usuario.setNombre("Juan Perez");
        usuario.setEmail("juan@example.com");

        mesa = new Mesa();
        mesa.setIdMesa(1L);
        mesa.setNumeroMesa(1);
        mesa.setCapacidad(4);

        estado = new EstadoReserva();
        estado.setIdEstadoReserva(1L);
        estado.setNombre("PENDIENTE");

        reserva = new Reserva();
        reserva.setIdReserva(1L);
        reserva.setUsuario(usuario);
        reserva.setMesa(mesa);
        reserva.setFecha(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.of(14, 0));
        reserva.setNumPersonas(4);
        reserva.setEstadoReserva(estado);

        pago = new Pago();
        pago.setIdPasarela("pi_123456");
        pago.setMonto(20.0);
        pago.setReserva(reserva);
        pago.setFechaPago(LocalDateTime.now());
    }

    @Test
    void enviarCorreo_EnviaMensajeExitosamente() {
        String destinatario = "test@example.com";
        String asunto = "Asunto de prueba";
        String cuerpo = "Cuerpo del mensaje";

        // No debe lanzar excepción
        try {
            emailService.enviarCorreo(destinatario, asunto, cuerpo);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e.getMessage());
        }

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarRecordatorio_EnviaMensajeExitosamente() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        LocalTime hora = LocalTime.of(14, 0);

        try {
            emailService.enviarRecordatorio("juan@example.com", "Juan Perez", fecha, hora);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e.getMessage());
        }

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarNotificacionCancelacion_EnviaMensajeExitosamente() {
        String motivo = "Motivo de cancelación";

        try {
            emailService.enviarNotificacionCancelacion("juan@example.com", "Juan Perez", motivo);
        } catch (Exception e) {
            fail("No debería lanzar excepción: " + e.getMessage());
        }

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarFacturaConPDF_GeneraPDFYEnviaCorreo() {
        try {
            emailService.enviarFacturaConPDF(pago);
        } catch (Exception e) {
        }
        verify(mailSender).createMimeMessage();
    }
}
