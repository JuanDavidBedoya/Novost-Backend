package com.restaurante.backend.services;

import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Mesa;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.exceptions.ValidationException;
import com.restaurante.backend.mappers.PagoMapper;
import com.restaurante.backend.mappers.ReservaMapper;
import com.restaurante.backend.repositories.EstadoReservaRepository;
import com.restaurante.backend.repositories.MesaRepository;
import com.restaurante.backend.repositories.PagoRepository;
import com.restaurante.backend.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private PagoRepository pagoRepo;

    @Mock
    private MesaRepository mesaRepo;

    @Mock
    private EstadoReservaRepository estadoRepo;

    @Mock
    private ReservaMapper reservaMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private PagoMapper pagoMapper;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario usuario;
    private Mesa mesa;
    private EstadoReserva estadoPendiente;
    private Reserva reserva;
    private ReservaRequestDTO reservaRequest;

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

        estadoPendiente = new EstadoReserva();
        estadoPendiente.setIdEstadoReserva(1L);
        estadoPendiente.setNombre("PENDIENTE");

        reserva = new Reserva();
        reserva.setIdReserva(1L);
        reserva.setUsuario(usuario);
        reserva.setMesa(mesa);
        reserva.setFecha(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.of(14, 0));
        reserva.setHoraFin(LocalTime.of(16, 0));
        reserva.setNumPersonas(4);
        reserva.setEstadoReserva(estadoPendiente);

        reservaRequest = new ReservaRequestDTO();
        reservaRequest.setCedulaUsuario("12345678");
        reservaRequest.setFecha(LocalDate.now().plusDays(1));
        reservaRequest.setHoraInicio(LocalTime.of(14, 0));
        reservaRequest.setNumPersonas(4);
    }

    @Test
    void crearReserva_HoraAntesDeApertura_ThrowsException() {
        reservaRequest.setHoraInicio(LocalTime.of(10, 0));

        when(reservaMapper.toEntity(reservaRequest)).thenReturn(reserva);

        ValidationException exception = assertThrows(ValidationException.class, 
            () -> reservaService.crearReserva(reservaRequest));

        assertTrue(exception.getMessage().contains("12:00"));
    }

    @Test
    void crearReserva_HoraDespuesDeCierre_ThrowsException() {
        reservaRequest.setHoraInicio(LocalTime.of(23, 59).plusSeconds(1));

        when(reservaMapper.toEntity(reservaRequest)).thenReturn(reserva);

        ValidationException exception = assertThrows(ValidationException.class, 
            () -> reservaService.crearReserva(reservaRequest));
        assertTrue(exception.getMessage().contains("12:00") || exception.getMessage().contains("00:00"));
    }

    @Test
    void crearReserva_NoHayMesasDisponibles_ThrowsException() {
        reserva.setHoraInicio(LocalTime.of(14, 0));
        
        when(reservaMapper.toEntity(reservaRequest)).thenReturn(reserva);
        when(mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(4)).thenReturn(List.of(mesa));
        when(reservaRepo.findOverlappingReservations(any(), any(), any(), any())).thenReturn(List.of());

        when(reservaRepo.findOverlappingReservations(eq(1L), any(), any(), any()))
            .thenReturn(List.of(new Reserva()));

        ValidationException exception = assertThrows(ValidationException.class, 
            () -> reservaService.crearReserva(reservaRequest));

        assertTrue(exception.getMessage().contains("No hay mesas disponibles"));
    }

    @Test
    void crearReserva_ReservaExitosa_ReturnsDTO() {
        EstadoReserva estadoPagada = new EstadoReserva();
        estadoPagada.setIdEstadoReserva(2L);
        estadoPagada.setNombre("PAGADA");
        
        when(reservaMapper.toEntity(reservaRequest)).thenReturn(reserva);
        when(mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(4)).thenReturn(List.of(mesa));
        when(reservaRepo.findOverlappingReservations(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(estadoRepo.findByNombre("PENDIENTE")).thenReturn(Optional.of(estadoPendiente));
        when(reservaRepo.save(any(Reserva.class))).thenReturn(reserva);
        
        ReservaResponseDTO responseDTO = new ReservaResponseDTO();
        responseDTO.setIdReserva(1L);
        when(reservaMapper.toResponseDTO(reserva)).thenReturn(responseDTO);

        ReservaResponseDTO result = reservaService.crearReserva(reservaRequest);

        assertNotNull(result);
        assertEquals(LocalTime.of(16, 0), reserva.getHoraFin());
        verify(emailService).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void buscarReservas_ConFiltros_ReturnsLista() {
        List<Reserva> reservas = List.of(reserva);
        ReservaResponseDTO dto = new ReservaResponseDTO();
        
        // Mock que acepta cualquier argumento
        when(reservaRepo.buscarTodasConFiltros(any(), any(), any())).thenReturn(reservas);
        when(reservaMapper.toDto(reserva)).thenReturn(dto);

        List<ReservaResponseDTO> result = reservaService.buscarReservas(
            LocalDate.now().plusDays(1), LocalTime.of(14, 0), 4);

        assertEquals(1, result.size());
    }

    @Test
    void buscarReservasPorUsuario_ReturnsLista() {
        List<Reserva> reservas = List.of(reserva);
        ReservaResponseDTO dto = new ReservaResponseDTO();
        
        when(reservaRepo.findByUsuarioCedula("12345678")).thenReturn(reservas);
        when(reservaMapper.toDto(reserva)).thenReturn(dto);

        List<ReservaResponseDTO> result = reservaService.buscarReservasPorUsuario("12345678");

        assertEquals(1, result.size());
    }

    @Test
    void procesarPagoReserva_ReservaNoEncontrada_ThrowsException() {
        when(reservaRepo.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> reservaService.procesarPagoReserva(999L, "pi_123", 20.0));

        assertTrue(exception.getMessage().contains("Reserva"));
    }

    @Test
    void procesarPagoReserva_PagoExitoso_ReturnsDTO() {
        EstadoReserva estadoPagada = new EstadoReserva();
        estadoPagada.setIdEstadoReserva(2L);
        estadoPagada.setNombre("PAGADA");
        
        Pago pago = new Pago();
        pago.setIdPasarela("pi_123");
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(estadoRepo.findByNombre("PAGADA")).thenReturn(Optional.of(estadoPagada));
        when(pagoRepo.save(any(Pago.class))).thenReturn(pago);
        when(reservaRepo.save(any(Reserva.class))).thenReturn(reserva);
        
        PagoResponseDTO pagoResponse = new PagoResponseDTO();
        pagoResponse.setIdReserva(1L);
        when(pagoMapper.toResponseDTO(pago)).thenReturn(pagoResponse);

        PagoResponseDTO result = reservaService.procesarPagoReserva(1L, "pi_123", 20.0);

        assertNotNull(result);
        verify(emailService).enviarFacturaConPDF(any(Pago.class));
    }

    @Test
    void cancelarReserva_ReservaNoEncontrada_ThrowsException() {
        when(reservaRepo.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> reservaService.cancelarReserva(999L));

        assertTrue(exception.getMessage().contains("Reserva"));
    }

    @Test
    void cancelarReserva_FechaPasada_ThrowsException() {
        reserva.setFecha(LocalDate.now().minusDays(1));
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));

        ValidationException exception = assertThrows(ValidationException.class, 
            () -> reservaService.cancelarReserva(1L));

        assertTrue(exception.getMessage().contains("fechas pasadas"));
    }

    @Test
    void cancelarReserva_CancelacionExitosa_ReturnsDTO() {
        EstadoReserva estadoCancelado = new EstadoReserva();
        estadoCancelado.setIdEstadoReserva(3L);
        estadoCancelado.setNombre("CANCELADA");
        
        when(reservaRepo.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagoRepo.findByReserva(reserva)).thenReturn(Optional.empty());
        when(estadoRepo.findByNombre("CANCELADA")).thenReturn(Optional.of(estadoCancelado));
        when(reservaRepo.save(any(Reserva.class))).thenReturn(reserva);
        
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setIdReserva(1L);
        when(reservaMapper.toDto(reserva)).thenReturn(dto);

        ReservaResponseDTO result = reservaService.cancelarReserva(1L);

        assertNotNull(result);
        verify(emailService).enviarCorreo(anyString(), anyString(), anyString());
    }

    // Tests para contarMesasDisponibles
    
    @Test
    void contarMesasDisponibles_SinFecha_RetornaTodasLasMesas() {
        List<Mesa> mesas = List.of(
            crearMesa(1L, 1, 4),
            crearMesa(2L, 2, 6),
            crearMesa(3L, 3, 2)
        );
        
        when(mesaRepo.findAll()).thenReturn(mesas);
        
        int resultado = reservaService.contarMesasDisponibles(null, null, null);
        
        assertEquals(3, resultado);
        verify(reservaRepo, never()).buscarReservasPorFecha(any());
    }
    
    @Test
    void contarMesasDisponibles_SoloFecha_RetornaTodasLasMesas() {
        List<Mesa> mesas = List.of(
            crearMesa(1L, 1, 4),
            crearMesa(2L, 2, 6)
        );
        
        when(mesaRepo.findAll()).thenReturn(mesas);
        
        // Cuando solo hay fecha (sin hora ni personas), retorna todas las mesas
        int resultado = reservaService.contarMesasDisponibles(LocalDate.now().plusDays(1), null, null);
        
        assertEquals(2, resultado); // Retorna todas las mesas porque no hay filtros específicos
        verify(reservaRepo, never()).buscarReservasPorFecha(any());
    }
    
    @Test
    void contarMesasDisponibles_FechaYHora_RetornaMesasDisponibles() {
        Mesa mesa1 = crearMesa(1L, 1, 4);
        Mesa mesa2 = crearMesa(2L, 2, 6);
        
        Reserva reservaOcupada = new Reserva();
        reservaOcupada.setMesa(mesa1);
        reservaOcupada.setFecha(LocalDate.now().plusDays(1));
        reservaOcupada.setHoraInicio(LocalTime.of(14, 0));
        reservaOcupada.setHoraFin(LocalTime.of(16, 0));
        
        when(mesaRepo.findAll()).thenReturn(List.of(mesa1, mesa2));
        when(reservaRepo.buscarReservasPorFecha(any())).thenReturn(List.of(reservaOcupada));
        
        int resultado = reservaService.contarMesasDisponibles(
            LocalDate.now().plusDays(1), 
            LocalTime.of(14, 0), 
            null
        );
        
        assertEquals(1, resultado); // Solo mesa2 está disponible a las 14:00
    }
    
    @Test
    void contarMesasDisponibles_FechaHoraYPersonas_RetornaMesasDisponibles() {
        Mesa mesa1 = crearMesa(1L, 1, 4);
        Mesa mesa2 = crearMesa(2L, 2, 6);
        Mesa mesa3 = crearMesa(3L, 3, 2);
        
        Reserva reservaOcupada = new Reserva();
        reservaOcupada.setMesa(mesa1);
        reservaOcupada.setFecha(LocalDate.now().plusDays(1));
        reservaOcupada.setHoraInicio(LocalTime.of(14, 0));
        reservaOcupada.setHoraFin(LocalTime.of(16, 0));
        
        when(mesaRepo.findAll()).thenReturn(List.of(mesa1, mesa2, mesa3));
        when(reservaRepo.buscarReservasPorFecha(any())).thenReturn(List.of(reservaOcupada));
        
        // Buscamos mesa para 5 personas a las 14:00
        int resultado = reservaService.contarMesasDisponibles(
            LocalDate.now().plusDays(1), 
            LocalTime.of(14, 0), 
            5
        );
        
        // Mesa1 está ocupada, Mesa3 solo tiene capacidad 2 (no sirve para 5)
        // Solo Mesa2 sirve (capacidad 6) y no está ocupada
        assertEquals(1, resultado);
    }
    
    @Test
    void contarMesasDisponibles_SinDisponibilidad_RetornaCero() {
        Mesa mesa1 = crearMesa(1L, 1, 4);
        
        Reserva reservaOcupada = new Reserva();
        reservaOcupada.setMesa(mesa1);
        reservaOcupada.setFecha(LocalDate.now().plusDays(1));
        reservaOcupada.setHoraInicio(LocalTime.of(14, 0));
        reservaOcupada.setHoraFin(LocalTime.of(16, 0));
        
        when(mesaRepo.findAll()).thenReturn(List.of(mesa1));
        when(reservaRepo.buscarReservasPorFecha(any())).thenReturn(List.of(reservaOcupada));
        
        int resultado = reservaService.contarMesasDisponibles(
            LocalDate.now().plusDays(1), 
            LocalTime.of(14, 0), 
            null
        );
        
        assertEquals(0, resultado);
    }
    
    private Mesa crearMesa(Long id, int numero, int capacidad) {
        Mesa mesa = new Mesa();
        mesa.setIdMesa(id);
        mesa.setNumeroMesa(numero);
        mesa.setCapacidad(capacidad);
        return mesa;
    }
}
