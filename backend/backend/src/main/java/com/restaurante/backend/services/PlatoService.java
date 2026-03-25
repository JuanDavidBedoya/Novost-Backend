package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.*;
import com.restaurante.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatoService {

    private final PlatoRepository           platoRepository;
    private final PlatoDetalleRepository    platoDetalleRepository;
    private final PlatoImagenRepository     platoImagenRepository;
    private final PlatoConfigRepository     platoConfigRepository;
    private final CategoriaRepository       categoriaRepository;
    private final InventarioRepository      inventarioRepository;

    // ──────────────────────────────────────────────────────────────
    //  CREAR PLATO
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public void crearPlato(CrearPlatoRequestDTO dto) {
        // 1. Buscar categoría
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // 2. Crear y guardar el Plato (estado true por defecto, se recalcula al listar)
        Plato plato = new Plato();
        plato.setNombrePlato(dto.getNombrePlato());
        plato.setDescripcion(dto.getDescripcion());
        plato.setPrecioPlato(dto.getPrecioPlato());
        plato.setCategoria(categoria);
        plato.setEstado(true);
        platoRepository.save(plato);

        // 3. Crear PlatoConfig (habilitado por defecto)
        PlatoConfig config = new PlatoConfig();
        config.setPlato(plato);
        config.setHabilitadoAdmin(true);
        platoConfigRepository.save(config);

        // 4. Guardar imagen
        if (dto.getImagenUrl() != null && !dto.getImagenUrl().isBlank()) {
            PlatoImagen imagen = new PlatoImagen();
            imagen.setPlato(plato);
            imagen.setImagenUrl(dto.getImagenUrl());
            platoImagenRepository.save(imagen);
        }

        // 5. Guardar ingredientes (plato_detalle)
        if (dto.getIngredientes() != null) {
            for (IngredienteDetalleDTO ing : dto.getIngredientes()) {
                Inventario inventario = inventarioRepository.findById(ing.getIdAlimento())
                        .orElseThrow(() -> new RuntimeException(
                                "Ingrediente no encontrado con id: " + ing.getIdAlimento()));
                PlatoDetalle detalle = new PlatoDetalle();
                detalle.setPlato(plato);
                detalle.setInventario(inventario);
                detalle.setCantidadNecesaria(ing.getCantidadNecesaria());
                platoDetalleRepository.save(detalle);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  TOGGLE HABILITADO ADMIN
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public boolean toggleHabilitadoAdmin(Long idPlato) {
        Plato plato = platoRepository.findById(idPlato)
                .orElseThrow(() -> new RuntimeException("Plato no encontrado"));

        PlatoConfig config = platoConfigRepository.findByPlato(plato)
                .orElseGet(() -> {
                    PlatoConfig c = new PlatoConfig();
                    c.setPlato(plato);
                    c.setHabilitadoAdmin(true);
                    return c;
                });

        config.setHabilitadoAdmin(!config.getHabilitadoAdmin());
        platoConfigRepository.save(config);
        return config.getHabilitadoAdmin();
    }

    // ──────────────────────────────────────────────────────────────
    //  BUSCAR INGREDIENTES
    // ──────────────────────────────────────────────────────────────
    public List<InventarioItemDTO> buscarIngredientes(String query) {
        return inventarioRepository
                .findByNombreAlimentoContainingIgnoreCase(query)
                .stream()
                .map(inv -> new InventarioItemDTO(
                        inv.getIdAlimento(),
                        inv.getNombreAlimento(),
                        inv.getTipoMedida().name()))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    //  OBTENER TODOS LOS PLATOS (panel admin)
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public List<PlatoAdminDTO> obtenerTodosLosPlatos() {
        return platoRepository.findAll().stream()
                .map(plato -> {
                    boolean habilitadoAdmin = platoConfigRepository.findByPlato(plato)
                            .map(PlatoConfig::getHabilitadoAdmin)
                            .orElse(true);

                    String imagenUrl = platoImagenRepository.findByPlato(plato)
                            .map(PlatoImagen::getImagenUrl)
                            .orElse(null);

                    return new PlatoAdminDTO(
                            plato.getIdPlato(),
                            plato.getNombrePlato(),
                            plato.getDescripcion(),
                            plato.getPrecioPlato(),
                            plato.getCategoria().getNombreCategoria(),
                            imagenUrl,
                            plato.getEstado(),
                            habilitadoAdmin
                    );
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    //  OBTENER PLATOS POR CATEGORÍA — ahora combina stock + admin
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public List<MenuItemDTO> obtenerPlatosPorCategoria(String categoria) {
        List<Plato> platos = platoRepository.findByCategoriaNombreCategoria(categoria);

        for (Plato plato : platos) {
            boolean disponibleReal = calcularDisponibilidad(plato);
            if (!plato.getEstado().equals(disponibleReal)) {
                plato.setEstado(disponibleReal);
                platoRepository.save(plato);
            }
        }

        return platos.stream()
                .map(plato -> {
                    boolean habilitadoAdmin = platoConfigRepository.findByPlato(plato)
                            .map(PlatoConfig::getHabilitadoAdmin)
                            .orElse(true);

                    // ✅ disponible = true solo si hay stock Y el admin lo habilitó
                    boolean disponibleFinal = plato.getEstado() && habilitadoAdmin;

                    String imagenUrl = platoImagenRepository.findByPlato(plato)
                            .map(PlatoImagen::getImagenUrl)
                            .orElse(null);

                    return new MenuItemDTO(
                            plato.getIdPlato(),
                            plato.getNombrePlato(),
                            plato.getDescripcion(),
                            plato.getPrecioPlato(),
                            disponibleFinal,
                            imagenUrl
                    );
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    //  Métodos existentes sin cambios
    // ──────────────────────────────────────────────────────────────
    private boolean calcularDisponibilidad(Plato plato) {
        List<PlatoDetalle> receta = platoDetalleRepository.findByPlato(plato);
        if (receta.isEmpty()) return true;
        for (PlatoDetalle ingrediente : receta) {
            if (ingrediente.getInventario().getStockActual() < ingrediente.getCantidadNecesaria()) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void recalcularDisponibilidadPorIngredientesAfectados(List<Plato> platosAfectados) {
        Set<Long> idsAlimentosAfectados = platosAfectados.stream()
            .flatMap(plato -> platoDetalleRepository.findByPlato(plato).stream())
            .map(pd -> pd.getInventario().getIdAlimento())
            .collect(Collectors.toSet());

        List<PlatoDetalle> detallesAfectados = platoDetalleRepository
            .findByInventarioIdAlimentoIn(idsAlimentosAfectados);

        Set<Plato> platosAReevaluar = detallesAfectados.stream()
            .map(PlatoDetalle::getPlato)
            .collect(Collectors.toSet());

        for (Plato plato : platosAReevaluar) {
            boolean disponibleReal = calcularDisponibilidad(plato);
            if (!plato.getEstado().equals(disponibleReal)) {
                plato.setEstado(disponibleReal);
                platoRepository.save(plato);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  GUARDAR URL DE IMAGEN (método existente sin cambios)
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public String guardarImagenUrl(Long idPlato, String imagenUrl) {
        Plato plato = platoRepository.findById(idPlato)
                .orElseThrow(() -> new RuntimeException("Plato no encontrado con id: " + idPlato));

        PlatoImagen platoImagen = platoImagenRepository
                .findByPlato(plato)
                .orElse(new PlatoImagen());
        platoImagen.setPlato(plato);
        platoImagen.setImagenUrl(imagenUrl);
        platoImagenRepository.save(platoImagen);
        return imagenUrl;
    }
}