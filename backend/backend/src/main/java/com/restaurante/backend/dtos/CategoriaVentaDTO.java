package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaVentaDTO {
    private String nombreCategoria;
    private Long   cantidadVendida;
    private Double porcentaje;      // % sobre el total de unidades vendidas en el día
    private Double ingresos;        // ingresos totales de la categoría
}