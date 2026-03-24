package com.restaurante.backend.mappers;

import com.restaurante.backend.dtos.InventarioRequestDTO;
import com.restaurante.backend.dtos.InventarioResponseDTO;
import com.restaurante.backend.entities.Inventario;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class InventarioMapper {

    // Mapear de Entity a ResponseDTO
    public InventarioResponseDTO toResponseDTO(Inventario inventario) {
        InventarioResponseDTO dto = new InventarioResponseDTO();
        dto.setIdAlimento(inventario.getIdAlimento());
        dto.setNombreAlimento(inventario.getNombreAlimento());
        dto.setTipoMedida(inventario.getTipoMedida());
        dto.setStockActual(inventario.getStockActual());
        dto.setStockMinimo(inventario.getStockMinimo());
        dto.setConsumoHoy(inventario.getConsumoHoy());
        dto.setUltimoConsumo(inventario.getUltimoConsumo());
        dto.setFechaActualizacion(inventario.getFechaActualizacion());
        dto.setBelowMinStock(inventario.getStockActual() < inventario.getStockMinimo());
        return dto;
    }

    // Mapear de RequestDTO a Entity 
    public Inventario toEntity(InventarioRequestDTO request) {
        Inventario inventario = new Inventario();
        inventario.setNombreAlimento(request.getNombreAlimento());
        inventario.setTipoMedida(request.getTipoMedida());
        inventario.setStockActual(request.getStockActual());
        inventario.setStockMinimo(request.getStockMinimo());
        inventario.setConsumoHoy(0.0);
        inventario.setUltimoConsumo(0.0);
        inventario.setFechaActualizacion(LocalDate.now());
        return inventario;
    }

    // Actualizar Entity desde RequestDTO
    public void updateEntity(Inventario inventario, InventarioRequestDTO request) {
        inventario.setNombreAlimento(request.getNombreAlimento());
        inventario.setTipoMedida(request.getTipoMedida());
        inventario.setStockActual(request.getStockActual());
        inventario.setStockMinimo(request.getStockMinimo());
        inventario.setFechaActualizacion(LocalDate.now());
    }
}
