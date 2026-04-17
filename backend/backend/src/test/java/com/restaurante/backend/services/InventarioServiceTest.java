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
 * Pruebas unitarias para InventarioService.
 * Verifica: creación, actualización, eliminación de productos,
 * gestión de stock, alertas de stock mínimo.
 */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock private InventarioRepository inventarioRepository;
    @Mock private InventarioMapper inventarioMapper;
    @Mock private EmailService emailService;
    @Mock private StockAlertaMetricaService stockAlertaMetricaService;
    @Mock private IntegridadStockMetricaService integridadStockMetricaService;
    @Mock private ModularidadInventarioMetricaService modularidadMetricaService;
    @Mock private AdaptabilidadInventarioMetricaService adaptabilidadMetricaService;

    @InjectMocks private InventarioService inventarioService;

    private Inventario inventario;

    @BeforeEach
    void setUp() {
        inventario = new Inventario();
        inventario.setIdAlimento(1L);
        inventario.setNombreAlimento("Tomate");
        inventario.setTipoMedida(Inventario.TipoMedida.KILO);
        inventario.setStockActual(50.0);
        inventario.setStockMinimo(10.0);
        inventario.setConsumoHoy(0.0);
        inventario.setUltimoConsumo(0.0);
    }

    @Test
    void crearProducto_deberiaCrearProducto_cuandoDatosSonValidos() {
        var request = new InventarioRequestDTO("Tomate", Inventario.TipoMedida.KILO, 50.0, 10.0);
        
        when(inventarioMapper.toEntity(request)).thenReturn(inventario);
        when(inventarioRepository.save(inventario)).thenReturn(inventario);
        when(inventarioMapper.toResponseDTO(inventario)).thenReturn(
            new InventarioResponseDTO());

        var response = inventarioService.crearProducto(request);
        
        assertNotNull(response);
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void actualizarProducto_deberiaActualizar_cuandoExiste() {
        var request = new InventarioRequestDTO("Tomate", Inventario.TipoMedida.KILO, 100.0, 20.0);
        
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));
        doNothing().when(inventarioMapper).updateEntity(inventario, request);
        when(inventarioRepository.save(inventario)).thenReturn(inventario);
        when(inventarioMapper.toResponseDTO(inventario)).thenReturn(new InventarioResponseDTO());

        var response = inventarioService.actualizarProducto(1L, request);
        
        assertNotNull(response);
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void actualizarProducto_deberiaLanzarExcepcion_cuandoNoExiste() {
        var request = new InventarioRequestDTO("Tomate", Inventario.TipoMedida.KILO, 50.0, 10.0);
        when(inventarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> inventarioService.actualizarProducto(999L, request));
    }

    @Test
    void eliminarProducto_deberiaEliminar_cuandoExiste() {
        when(inventarioRepository.existsById(1L)).thenReturn(true);
        doNothing().when(inventarioRepository).deleteById(1L);

        assertDoesNotThrow(() -> inventarioService.eliminarProducto(1L));
        verify(inventarioRepository).deleteById(1L);
    }

    @Test
    void eliminarProducto_deberiaLanzarExcepcion_cuandoNoExiste() {
        when(inventarioRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, 
            () -> inventarioService.eliminarProducto(999L));
    }

    @Test
    void obtenerTodos_deberiaRetornarLista_deProductos() {
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario));
        when(inventarioMapper.toResponseDTO(any())).thenReturn(new InventarioResponseDTO());

        var response = inventarioService.obtenerTodos();
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void obtenerPorId_deberiaRetornarProducto_cuandoExiste() {
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));
        when(inventarioMapper.toResponseDTO(inventario)).thenReturn(new InventarioResponseDTO());

        var response = inventarioService.obtenerPorId(1L);
        
        assertNotNull(response);
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcion_cuandoNoExiste() {
        when(inventarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> inventarioService.obtenerPorId(999L));
    }

    @Test
    void agregarStock_deberiaAumentarStock_cuandoCantidadPositiva() {
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(inventario)).thenReturn(inventario);
        when(inventarioMapper.toResponseDTO(any())).thenReturn(new InventarioResponseDTO());

        var response = inventarioService.agregarStock(1L, 20.0);
        
        assertNotNull(response);
        assertEquals(70.0, inventario.getStockActual());
    }

    @Test
    void agregarStock_deberiaLanzarExcepcion_cuandoCantidadCero() {
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));

        assertThrows(ValidationException.class, 
            () -> inventarioService.agregarStock(1L, 0.0));
    }

    @Test
    void quitarStock_deberiaReducirStock_cuandoHaySuficiente() {
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));
        when(inventarioRepository.save(inventario)).thenReturn(inventario);
        when(inventarioMapper.toResponseDTO(any())).thenReturn(new InventarioResponseDTO());

        var response = inventarioService.quitarStock(1L, 20.0);
        
        assertNotNull(response);
        assertEquals(30.0, inventario.getStockActual());
    }

    @Test
    void quitarStock_deberiaLanzarExcepcion_cuandoStockInsuficiente() {
        inventario.setStockActual(10.0);
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inventario));

        assertThrows(ValidationException.class, 
            () -> inventarioService.quitarStock(1L, 20.0));
    }
}