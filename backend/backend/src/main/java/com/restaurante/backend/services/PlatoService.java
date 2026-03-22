package com.restaurante.backend.services;

import com.restaurante.backend.dtos.MenuItemDTO;
import com.restaurante.backend.entities.Plato;
import com.restaurante.backend.entities.PlatoDetalle;
import com.restaurante.backend.repositories.PlatoDetalleRepository;
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

    @Transactional
    public List<MenuItemDTO> obtenerPlatosPorCategoria(String categoria) {
        // 1. Traer todos los platos de la categoría sin filtrar por estado
        List<Plato> platos = platoRepository.findByCategoriaNombreCategoria(categoria);

        // 2. Por cada plato, calcular si realmente está disponible
        for (Plato plato : platos) {
            boolean disponibleReal = calcularDisponibilidad(plato);

            // 3. Si el estado en BD no coincide con la realidad, actualizarlo
            if (!plato.getEstado().equals(disponibleReal)) {
                plato.setEstado(disponibleReal);
                platoRepository.save(plato);
            }
        }

        // 4. Mapear a DTO con el estado ya actualizado
        return platos.stream()
                .map(plato -> new MenuItemDTO(
                        plato.getIdPlato(),
                        plato.getNombrePlato(),
                        plato.getDescripcion(),
                        plato.getPrecioPlato(),
                        plato.getEstado()))
                .collect(Collectors.toList());
    }

    private boolean calcularDisponibilidad(Plato plato) {
        List<PlatoDetalle> receta = platoDetalleRepository.findByPlato(plato);

        // Si el plato no tiene receta registrada, se considera disponible
        if (receta.isEmpty()) {
            return true;
        }

        // Verificar que TODOS los ingredientes tengan stock suficiente para al menos 1 porción
        for (PlatoDetalle ingrediente : receta) {
            double stockActual = ingrediente.getInventario().getStockActual();
            double cantidadNecesaria = ingrediente.getCantidadNecesaria();

            if (stockActual < cantidadNecesaria) {
                return false; // Con que un ingrediente falte, el plato no está disponible
            }
        }

        return true;
    }

    @Transactional
    public void recalcularDisponibilidadPorIngredientesAfectados(List<Plato> platosAfectados) {
        // 1. Recolectar todos los id_alimento usados en los platos del pedido
        Set<Long> idsAlimentosAfectados = platosAfectados.stream()
            .flatMap(plato -> platoDetalleRepository.findByPlato(plato).stream())
            .map(pd -> pd.getInventario().getIdAlimento())
            .collect(Collectors.toSet());

        // 2. Buscar TODOS los platos que usen alguno de esos ingredientes
        List<PlatoDetalle> detallesAfectados = platoDetalleRepository
            .findByInventarioIdAlimentoIn(idsAlimentosAfectados);

        Set<Plato> platosAReevaluar = detallesAfectados.stream()
            .map(PlatoDetalle::getPlato)
            .collect(Collectors.toSet());

        // 3. Recalcular disponibilidad de cada plato afectado
        for (Plato plato : platosAReevaluar) {
            boolean disponibleReal = calcularDisponibilidad(plato);
            if (!plato.getEstado().equals(disponibleReal)) {
                plato.setEstado(disponibleReal);
                platoRepository.save(plato);
            }
        }
    }
}