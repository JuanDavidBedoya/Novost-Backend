package com.restaurante.backend.mappers;

import com.restaurante.backend.dtos.ProductoIndividualDTO;
import com.restaurante.backend.entities.ProductoIndividual;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class ProductoIndividualMapper {

    public ProductoIndividualDTO toDTO(ProductoIndividual entity) {
        ProductoIndividualDTO dto = new ProductoIndividualDTO();
        dto.setIdProducto(entity.getIdProducto());
        dto.setIdAlimento(entity.getInventario().getIdAlimento());
        dto.setNombreAlimento(entity.getInventario().getNombreAlimento());
        dto.setCantidad(entity.getCantidad());
        dto.setTipoMedida(entity.getInventario().getTipoMedida().name());
        dto.setFechaVencimiento(entity.getFechaVencimiento());
        dto.setLote(entity.getLote());
        dto.setFechaIngreso(entity.getFechaIngreso());
        dto.setPrecioCompra(entity.getPrecioCompra());
        dto.setProveedor(entity.getProveedor());
        dto.setEstado(entity.getEstado());
        dto.setCedulaTrabajador(entity.getCedulaTrabajador());
        
        long dias = LocalDate.now().until(entity.getFechaVencimiento()).getDays();
        dto.setDiasParaVencer(dias);
        dto.setProximoAVencer(dias <= 30 && dias > 0);
        
        return dto;
    }
}