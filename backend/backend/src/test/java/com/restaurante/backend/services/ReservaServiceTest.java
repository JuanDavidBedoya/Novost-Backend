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

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Pruebas unitarias para ReservaService.
 * Verifica: creación de reservas, búsqueda de reservas, procesamiento de pagos,
 * cancelación y finalización de reservas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepo;
    @Mock private PagoRepository pagoRepo;
    @Mock private MesaRepository mesaRepo;
    @Mock private EstadoReservaRepository estadoRepo;
    @Mock private CargaConcurrenteMetricaService cargaConcurrenteMetricaService;
    @Mock private AsignacionMesaMetricaService asignacionMetricaService;
    @Mock private ConversionReservaMetricaService conversionMetricaService;
    @Mock private StripeMetricaService stripeMetricaService;
    @Mock private ReservaMapper reservaMapper;
    @Mock private EmailService emailService;
    @Mock private PagoMapper pagoMapper;

    @InjectMocks private ReservaService reservaService;

    private Reserva reserva;
    private Mesa mesa;

    @BeforeEach
    void setUp() {
        mesa = new Mesa();
        mesa.setCapacidad(4);
        
        reserva = new Reserva();
        reserva.setFecha(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.of(19, 0));
        reserva.setHoraFin(LocalTime.of(21, 0));
        
        var usuario = new Usuario();
        usuario.setEmail("test@test.com");
        reserva.setUsuario(usuario);
    }

    @Test
    void crearReserva_deberiaCrearReserva_cuandoDatosSonValidos() {
        reserva.setNumPersonas(4);
        
        var dto = mock(ReservaRequestDTO.class);
        when(dto.getHoraInicio()).thenReturn(LocalTime.of(19, 0));
        when(dto.getNumPersonas()).thenReturn(4);
        when(dto.getCedulaUsuario()).thenReturn("12345678");
        when(dto.getFecha()).thenReturn(LocalDate.now().plusDays(1));

        when(reservaMapper.toEntity(dto)).thenReturn(reserva);
        when(mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(4)).thenReturn(java.util.List.of(mesa));
        when(reservaRepo.findOverlappingReservations(any(), any(), any(), any())).thenReturn(java.util.List.of());
        when(estadoRepo.findByNombre("PENDIENTE")).thenReturn(Optional.of(new EstadoReserva()));
        when(reservaRepo.save(any())).thenReturn(reserva);
        when(reservaMapper.toResponseDTO(any())).thenReturn(new ReservaResponseDTO());

        var result = reservaService.crearReserva(dto);
        
        assertNotNull(result);
    }

    @Test
    void crearReserva_deberiaLanzarExcepcion_cuandoHoraEsAntesDeApertura() {
        var dto = mock(ReservaRequestDTO.class);
        when(dto.getHoraInicio()).thenReturn(LocalTime.of(10, 0));

        var ex = assertThrows(ValidationException.class, () -> reservaService.crearReserva(dto));
        assertTrue(ex.getMessage().contains("hora de reserva"));
    }

    @Test
    void crearReserva_deberiaLanzarExcepcion_cuandoNoHayMesasDisponibles() {
        var dto = mock(ReservaRequestDTO.class);
        when(dto.getHoraInicio()).thenReturn(LocalTime.of(19, 0));
        when(dto.getNumPersonas()).thenReturn(10);

        when(reservaMapper.toEntity(dto)).thenReturn(reserva);
        when(mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(10)).thenReturn(List.of(mesa));
        when(reservaRepo.findOverlappingReservations(any(), any(), any(), any())).thenReturn(List.of(new Reserva()));

        var ex = assertThrows(ValidationException.class, () -> reservaService.crearReserva(dto));
        assertTrue(ex.getMessage().contains("No hay mesas disponibles"));
    }

    @Test
    void buscarReservas_deberiaRetornarReservas() {
        when(reservaRepo.buscarTodasConFiltros(any(), any(), any())).thenReturn(List.of(reserva));
        when(reservaMapper.toDto(any())).thenReturn(new ReservaResponseDTO());

        var result = reservaService.buscarReservas(LocalDate.now(), LocalTime.now(), 4);
        
        assertNotNull(result);
    }

    @Test
    void contarMesasDisponibles_deberiaRetornarNumero_deMesas() {
        when(mesaRepo.findAll()).thenReturn(java.util.List.of(mesa));
        when(reservaRepo.buscarReservasPorFecha(any())).thenReturn(java.util.List.of());

        var result = reservaService.contarMesasDisponibles(LocalDate.now(), null, null);
        
        assertTrue(result >= 0);
    }

    @Test
    void procesarPagoReserva_deberiaProcesarPago_cuandoReservaExiste() {
        reserva.setNumPersonas(4);
        
        var estadoPagado = new EstadoReserva();
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(estadoRepo.findByNombre("PAGADA")).thenReturn(Optional.of(estadoPagado));
        when(pagoRepo.save(any())).thenReturn(new Pago());
        when(reservaRepo.save(reserva)).thenReturn(reserva);
        when(pagoMapper.toResponseDTO(any())).thenReturn(new PagoResponseDTO());

        var result = reservaService.procesarPagoReserva(1L, "pi_123", 20.0);
        
        assertNotNull(result);
    }

    @Test
    void cancelarReserva_deberiaCancelarReserva_cuandoExiste() {
        var estadoPendiente = new EstadoReserva();
        estadoPendiente.setNombre("PENDIENTE");
        reserva.setEstadoReserva(estadoPendiente);
        
        var estadoCancelado = new EstadoReserva();
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoRepo.findByReserva(reserva)).thenReturn(Optional.empty());
        when(estadoRepo.findByNombre("CANCELADA")).thenReturn(Optional.of(estadoCancelado));
        when(reservaRepo.save(reserva)).thenReturn(reserva);
        when(reservaMapper.toDto(any())).thenReturn(new ReservaResponseDTO());

        var result = reservaService.cancelarReserva(1L);
        
        assertNotNull(result);
    }

    @Test
    void cancelarReserva_deberiaLanzarExcepcion_cuandoReservaEstaFinalizada() {
        var estadoFinalizada = new EstadoReserva();
        estadoFinalizada.setNombre("FINALIZADA");
        reserva.setEstadoReserva(estadoFinalizada);
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> reservaService.cancelarReserva(1L));
    }

    @Test
    void finalizarReserva_deberiaFinalizarReserva_cuandoEstaPagada() {
        var estadoPagado = new EstadoReserva();
        estadoPagado.setNombre("PAGADA");
        reserva.setEstadoReserva(estadoPagado);
        
        var estadoFinalizado = new EstadoReserva();
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(estadoRepo.findByNombre("FINALIZADA")).thenReturn(Optional.of(estadoFinalizado));
        when(reservaRepo.save(reserva)).thenReturn(reserva);
        when(reservaMapper.toDto(any())).thenReturn(new ReservaResponseDTO());

        var result = reservaService.finalizarReserva(1L);
        
        assertNotNull(result);
    }

    @Test
    void finalizarReserva_deberiaLanzarExcepcion_cuandoNoEstaPagada() {
        var estadoPendiente = new EstadoReserva();
        estadoPendiente.setNombre("PENDIENTE");
        reserva.setEstadoReserva(estadoPendiente);
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> reservaService.finalizarReserva(1L));
    }
}