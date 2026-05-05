package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TipoProductoDTO {
    private Long idTipo;
    private String nombreTipo;
    private String descripcion;
    private Boolean activo;
}