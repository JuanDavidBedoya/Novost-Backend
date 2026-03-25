package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrearPlatoRequestDTO {
    private String nombrePlato;
    private String descripcion;
    private Double precioPlato;
    private Long idCategoria;
    private String imagenUrl;
    private List<IngredienteDetalleDTO> ingredientes;
}