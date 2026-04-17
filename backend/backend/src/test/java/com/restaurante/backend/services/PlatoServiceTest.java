package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.*;
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
 * Pruebas unitarias para PlatoService.
 * Verifica: creación de platos, toggle de habilitación, búsqueda de ingredientes,
 * listado de platos por categoría y para admin.
 */
@ExtendWith(MockitoExtension.class)
class PlatoServiceTest {

    @Mock private PlatoRepository platoRepository;
    @Mock private PlatoDetalleRepository platoDetalleRepository;
    @Mock private PlatoImagenRepository platoImagenRepository;
    @Mock private PlatoConfigRepository platoConfigRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private InventarioRepository inventarioRepository;

    @InjectMocks private PlatoService platoService;

    private Plato plato;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setNombreCategoria("BEBIDAS");
        
        plato = new Plato();
        plato.setCategoria(categoria);
        plato.setEstado(true);
    }

    @Test
    void crearPlato_deberiaCrearPlato_cuandoCategoriaExiste() {
        var request = mock(CrearPlatoRequestDTO.class);
        when(request.getNombrePlato()).thenReturn("Agua Mineral");
        when(request.getDescripcion()).thenReturn("Agua mineral natural");
        when(request.getPrecioPlato()).thenReturn(2.50);
        when(request.getIdCategoria()).thenReturn(1L);
        when(request.getImagenUrl()).thenReturn(null);
        when(request.getIngredientes()).thenReturn(null);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(platoRepository.save(any(Plato.class))).thenReturn(plato);
        when(platoConfigRepository.save(any())).thenReturn(new PlatoConfig());

        assertDoesNotThrow(() -> platoService.crearPlato(request));
        verify(platoRepository).save(any(Plato.class));
    }

    @Test
    void crearPlato_deberiaLanzarExcepcion_cuandoCategoriaNoExiste() {
        var request = mock(CrearPlatoRequestDTO.class);
        when(request.getIdCategoria()).thenReturn(999L);

        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> platoService.crearPlato(request));
    }

    @Test
    void toggleHabilitadoAdmin_deberiaCambiarEstado() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(plato));
        when(platoConfigRepository.findByPlato(plato)).thenReturn(Optional.empty());
        when(platoConfigRepository.save(any())).thenReturn(new PlatoConfig());

        var result = platoService.toggleHabilitadoAdmin(1L);
        
        assertNotNull(result);
    }

    @Test
    void buscarIngredientes_deberiaRetornarIngredientes() {
        var inventario = new Inventario();
        inventario.setTipoMedida(Inventario.TipoMedida.KILO);
        when(inventarioRepository.findByNombreAlimentoContainingIgnoreCase("tomate"))
            .thenReturn(List.of(inventario));

        var result = platoService.buscarIngredientes("tomate");
        
        assertNotNull(result);
    }

    @Test
    void obtenerTodosLosPlatos_deberiaRetornarLista_dePlatos() {
        when(platoRepository.findAll()).thenReturn(List.of(plato));
        when(platoConfigRepository.findByPlato(plato)).thenReturn(Optional.of(new PlatoConfig()));
        when(platoImagenRepository.findByPlato(plato)).thenReturn(Optional.empty());

        var result = platoService.obtenerTodosLosPlatos();
        
        assertNotNull(result);
    }

    @Test
    void obtenerPlatosPorCategoria_deberiaRetornarPlatos() {
        when(platoRepository.findByCategoriaNombreCategoria("Bebidas")).thenReturn(List.of(plato));
        when(platoConfigRepository.findByPlato(plato)).thenReturn(Optional.of(new PlatoConfig()));
        when(platoImagenRepository.findByPlato(plato)).thenReturn(Optional.empty());
        when(platoDetalleRepository.findByPlato(plato)).thenReturn(List.of());

        var result = platoService.obtenerPlatosPorCategoria("Bebidas");
        
        assertNotNull(result);
    }
}