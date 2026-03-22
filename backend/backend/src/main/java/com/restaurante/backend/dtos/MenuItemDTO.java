package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemDTO {
    
    private Long idPlato;
    private String nombrePlato;
    private String descripcion;
    private Double precioPlato;
    private Boolean disponible;
    
}