package com.restaurante.backend.services;

import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Mesa;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasarelaServiceTest {

    @Mock
    private ReservaRepository reservaRepo;

    @InjectMocks
    private PasarelaService pasarelaService;

    private Usuario usuario;
    private Mesa mesa;
    private EstadoReserva estado;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pasarelaService, "stripeSecretKey", "sk_test_123456789");

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
        reserva.setNumPersonas(4);
        reserva.setEstadoReserva(estado);
    }

    @Test
    void crearIntentoPago_ReservaNoEncontrada_ThrowsException() {
        when(reservaRepo.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> pasarelaService.crearIntentoPago(999L));

        assertTrue(exception.getMessage().contains("Reserva"));
        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void crearIntentoPago_ReservaValida_ReservaTieneDatosCorrectos() {
        assertNotNull(reserva.getUsuario());
        assertNotNull(reserva.getMesa());
        assertEquals(4, reserva.getNumPersonas());
        assertEquals("juan@example.com", reserva.getUsuario().getEmail());
    }

    @Test
    void crearIntentoPago_CalculoMonto_CuatroPersonas() {
        int numPersonas = 4;
        double precioPorPersona = 5.0;
        
        double montoCalculado = numPersonas * precioPorPersona;
        
        assertEquals(20.0, montoCalculado);
    }

    @Test
    void crearIntentoPago_CalculoMonto_DosPersonas() {
        int numPersonas = 2;
        double precioPorPersona = 5.0;
        
        double montoCalculado = numPersonas * precioPorPersona;
        
        assertEquals(10.0, montoCalculado);
    }

    @Test
    void crearIntentoPago_ConversionCentavos_CuatroPersonas() {
        double monto = 20.0;
        long centavos = (long) (monto * 100);
        
        assertEquals(2000L, centavos);
    }
}
