package com.restaurante.backend.services;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.mappers.PagoMapper;
import com.restaurante.backend.repositories.EstadoReservaRepository;
import com.restaurante.backend.repositories.PagoRepository;
import com.restaurante.backend.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepo;

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private EstadoReservaRepository estadoRepo;

    @Mock
    private PagoMapper pagoMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PagoService pagoService;

    private Usuario usuario;
    private Reserva reserva;
    private EstadoReserva estadoPendiente;
    private EstadoReserva estadoPagada;
    private PagoRequestDTO pagoRequest;
    private Pago pago;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setCedula("12345678");
        usuario.setNombre("Juan Perez");
        usuario.setEmail("juan@example.com");

        reserva = new Reserva();
        reserva.setIdReserva(1L);
        reserva.setUsuario(usuario);
        reserva.setNumPersonas(4);

        estadoPendiente = new EstadoReserva();
        estadoPendiente.setIdEstadoReserva(1L);
        estadoPendiente.setNombre("PENDIENTE");

        estadoPagada = new EstadoReserva();
        estadoPagada.setIdEstadoReserva(2L);
        estadoPagada.setNombre("PAGADA");

        reserva.setEstadoReserva(estadoPendiente);

        pagoRequest = new PagoRequestDTO();
        pagoRequest.setIdReserva(1L);
        pagoRequest.setIdPasarela("pi_123456");
        pagoRequest.setMonto(20.0);

        pago = new Pago();
        pago.setIdPasarela("pi_123456");
        pago.setMonto(20.0);
        pago.setReserva(reserva);
    }

    @Test
    void procesarConfirmacionPago_ReservaNoEncontrada_ThrowsException() {
        pagoRequest.setIdReserva(999L);
        when(reservaRepo.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> pagoService.procesarConfirmacionPago(pagoRequest));

        assertTrue(exception.getMessage().contains("Reserva"));
    }

    @Test
    void procesarConfirmacionPago_EstadoNoEncontrado_ThrowsException() {
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoMapper.toEntity(pagoRequest)).thenReturn(pago);
        when(estadoRepo.findByNombre("PAGADA")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> pagoService.procesarConfirmacionPago(pagoRequest));

        assertTrue(exception.getMessage().contains("PAGADA"));
    }

    @Test
    void procesarConfirmacionPago_PagoExitoso_ReturnsDTO() {
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoMapper.toEntity(pagoRequest)).thenReturn(pago);
        when(estadoRepo.findByNombre("PAGADA")).thenReturn(Optional.of(estadoPagada));
        when(pagoRepo.save(any(Pago.class))).thenReturn(pago);
        when(reservaRepo.save(any(Reserva.class))).thenReturn(reserva);
        
        PagoResponseDTO pagoResponse = new PagoResponseDTO();
        pagoResponse.setIdReserva(1L);
        pagoResponse.setMonto(20.0);
        when(pagoMapper.toResponseDTO(pago)).thenReturn(pagoResponse);

        PagoResponseDTO result = pagoService.procesarConfirmacionPago(pagoRequest);

        assertNotNull(result);
        assertEquals(1L, result.getIdReserva());
        assertEquals("succeeded", pago.getEstadoPago());
        verify(emailService).enviarFacturaConPDF(any(Pago.class));
        verify(reservaRepo).save(argThat(r -> r.getEstadoReserva().getNombre().equals("PAGADA")));
    }
}
