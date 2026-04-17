package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.*;
import com.restaurante.backend.exceptions.*;
import com.restaurante.backend.mappers.*;
import com.restaurante.backend.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PagoService.
 * Verifica: procesamiento de pagos de reservas, confirmación de pagos.
 */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock private PagoRepository pagoRepo;
    @Mock private ReservaRepository reservaRepo;
    @Mock private EstadoReservaRepository estadoRepo;
    @Mock private PagoMapper pagoMapper;
    @Mock private EmailService emailService;

    @InjectMocks private PagoService pagoService;

    private Reserva reserva;
    private Pago pago;

    @BeforeEach
    void setUp() {
        reserva = new Reserva();
        
        pago = new Pago();
        pago.setIdPago(1L);
    }

    @Test
    void procesarConfirmacionPago_deberiaConfirmarPago_cuandoReservaExiste() {
        var dto = new PagoRequestDTO();
        dto.setIdReserva(1L);
        dto.setIdPasarela("pi_123456");
        dto.setMonto(20.0);

        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoMapper.toEntity(dto)).thenReturn(pago);
        when(pagoRepo.save(any(Pago.class))).thenReturn(pago);
        when(estadoRepo.findByNombre("PAGADA")).thenReturn(Optional.of(new EstadoReserva()));
        when(reservaRepo.save(reserva)).thenReturn(reserva);
        when(pagoMapper.toResponseDTO(any())).thenReturn(new PagoResponseDTO());

        var result = pagoService.procesarConfirmacionPago(dto);

        assertNotNull(result);
        verify(pagoRepo).save(any(Pago.class));
        verify(emailService).enviarFacturaConPDF(any(Pago.class));
    }

    @Test
    void procesarConfirmacionPago_deberiaLanzarExcepcion_cuandoReservaNoExiste() {
        var dto = new PagoRequestDTO();
        dto.setIdReserva(999L);
        
        when(reservaRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> pagoService.procesarConfirmacionPago(dto));
    }
}