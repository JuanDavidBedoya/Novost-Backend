package com.restaurante.backend.services;

import com.restaurante.backend.dtos.MenuItemDTO;
import com.restaurante.backend.entities.Plato;
import com.restaurante.backend.entities.PlatoDetalle;
import com.restaurante.backend.entities.PlatoImagen;
import com.restaurante.backend.repositories.PlatoDetalleRepository;
import com.restaurante.backend.repositories.PlatoImagenRepository;
import com.restaurante.backend.repositories.PlatoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatoService {

    private final PlatoRepository platoRepository;
    private final PlatoDetalleRepository platoDetalleRepository;
    private final PlatoImagenRepository platoImagenRepository; // ✅ Nuevo

    // ──────────────────────────────────────────────────────────────
    //  GUARDAR URL DE IMAGEN
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

    // ──────────────────────────────────────────────────────────────
    //  OBTENER PLATOS POR CATEGORÍA (incluye imagenUrl)
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
                    String imagenUrl = platoImagenRepository.findByPlato(plato)
                            .map(PlatoImagen::getImagenUrl)
                            .orElse(null);

                    return new MenuItemDTO(
                            plato.getIdPlato(),
                            plato.getNombrePlato(),
                            plato.getDescripcion(),
                            plato.getPrecioPlato(),
                            plato.getEstado(),
                            imagenUrl // ✅
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
}