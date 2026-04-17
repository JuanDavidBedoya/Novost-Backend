package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.*;
import com.restaurante.backend.exceptions.*;
import com.restaurante.backend.mappers.PedidoMapper;
import com.restaurante.backend.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Pruebas unitarias para PedidoService.
 * Verifica: creación de pedidos, confirmación de pagos, avance de estados,
 * consulta de pedidos del usuario y todos los pedidos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepo;
    @Mock private PedidoDetalleRepository pedidoDetalleRepo;
    @Mock private MesaRepository mesaRepo;
    @Mock private ReservaRepository reservaRepo;
    @Mock private PlatoRepository platoRepo;
    @Mock private PlatoDetalleRepository platoDetalleRepo;
    @Mock private InventarioRepository inventarioRepo;
    @Mock private EstadoPedidoRepository estadoPedidoRepo;
    @Mock private PedidoMapper pedidoMapper;
    @Mock private PlatoService platoService;
    @Mock private PagoPedidoRepository pagoPedidoRepo;
    @Mock private EmailService emailService;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private PedidoMetricaService pedidoMetricaService;

    @InjectMocks private PedidoService pedidoService;

    @SuppressWarnings("unused")
    private Mesa mesa;
    @SuppressWarnings("unused")
    private Plato plato;
    private Pedido pedido;
    @SuppressWarnings("unused")
    private Usuario usuario;

@SuppressWarnings("unused")
@BeforeEach
    void setUp() {
        doNothing().when(pedidoMetricaService).registrarIntento();
        
        var timer = mock(io.micrometer.core.instrument.Timer.class);
        doReturn(null).when(pedidoMetricaService).getTiempoTimer();
        
        mesa = new Mesa();
        plato = new Plato();
        usuario = new Usuario();
        pedido = new Pedido();
    }

    @Test
    void crearPedido_deberiaLanzarExcepcion_cuandoMesaNoExiste() {
        var dto = mock(PedidoRequestDTO.class);
        when(dto.getIdMesa()).thenReturn(999L);
        
        lenient().when(mesaRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> pedidoService.crearPedido(dto));
    }

    @Test
    void confirmarPagoPedido_deberiaConfirmarPago_cuandoPedidoExiste() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagoPedidoRepo.findByPedido(pedido)).thenReturn(Optional.of(new PagoPedido()));
        when(estadoPedidoRepo.findByNombre("PAGADO")).thenReturn(Optional.of(new EstadoPedido()));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);

        assertDoesNotThrow(() -> pedidoService.confirmarPagoPedido(1L, "pi_123"));
        verify(pagoPedidoRepo).save(any(PagoPedido.class));
    }

    @Test
    void confirmarPagoPedido_deberiaLanzarExcepcion_cuandoPedidoNoExiste() {
        when(pedidoRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> pedidoService.confirmarPagoPedido(999L, "pi_123"));
    }

    @Test
    void avanzarEstadoPedido_deberiaAvanzarEstado_cuandoEstadoEsRecibido() {
        var estado = new EstadoPedido();
        estado.setNombre("RECIBIDO");
        pedido.setEstadoPedido(estado);
        
        var estadoPagado = new EstadoPedido();
        estadoPagado.setNombre("PAGADO");

        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));
        when(estadoPedidoRepo.findByNombre("PAGADO")).thenReturn(Optional.of(estadoPagado));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponseDTO(any())).thenReturn(new PedidoResponseDTO());

        var result = pedidoService.avanzarEstadoPedido(1L);
        
        assertNotNull(result);
    }

    @Test
    void avanzarEstadoPedido_deberiaLanzarExcepcion_cuandoEstadoNoEsValido() {
        var estado = new EstadoPedido();
        estado.setNombre("ENTREGADO");
        pedido.setEstadoPedido(estado);
        
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(Exception.class, () -> pedidoService.avanzarEstadoPedido(1L));
    }

    @Test
    void obtenerTodosLosPedidos_deberiaRetornarTodosLosPedidos() {
        when(pedidoRepo.findAllWithFilters(any(), any())).thenReturn(java.util.List.of(pedido));
        when(pedidoMapper.toResponseDTO(any())).thenReturn(new PedidoResponseDTO());

        var result = pedidoService.obtenerTodosLosPedidos(null, null);
        
        assertNotNull(result);
    }
}