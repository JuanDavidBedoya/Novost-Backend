package com.restaurante.backend.mappers;

import com.restaurante.backend.dtos.InventarioRequestDTO;
import com.restaurante.backend.dtos.InventarioResponseDTO;
import com.restaurante.backend.dtos.TipoProductoDTO;
import com.restaurante.backend.entities.Inventario;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class InventarioMapper {

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
        
        if (inventario.getTipoProducto() != null) {
            TipoProductoDTO tipoDto = new TipoProductoDTO();
            tipoDto.setIdTipo(inventario.getTipoProducto().getIdTipo());
            tipoDto.setNombreTipo(inventario.getTipoProducto().getNombreTipo());
            tipoDto.setDescripcion(inventario.getTipoProducto().getDescripcion());
            tipoDto.setActivo(inventario.getTipoProducto().getActivo());
            dto.setTipoProducto(tipoDto);
        }
        
        return dto;
    }

    public Inventario toEntity(InventarioRequestDTO request) {
        Inventario inventario = new Inventario();
        inventario.setNombreAlimento(request.getNombreAlimento());
        inventario.setTipoMedida(request.getTipoMedida());
        inventario.setStockActual(0.0);
        inventario.setStockMinimo(request.getStockMinimo());
        inventario.setConsumoHoy(0.0);
        inventario.setUltimoConsumo(0.0);
        inventario.setFechaActualizacion(LocalDate.now());
        return inventario;
    }

    public void updateEntity(Inventario inventario, InventarioRequestDTO request) {
        inventario.setNombreAlimento(request.getNombreAlimento());
        inventario.setTipoMedida(request.getTipoMedida());
        // NO actualizar stockActual aquí – se maneja por endpoints agregar/quitar stock
        inventario.setStockMinimo(request.getStockMinimo());
        inventario.setFechaActualizacion(LocalDate.now());

        // Actualizar tipo de producto si se proporciona
        if (request.getIdTipo() != null) {
            // La relación se establece en el servicio
        }
    }
}
