package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoVentaDTO {
    private String nombrePlato;
    private Long   cantidadVendida;
    private Double porcentaje;      // % sobre el total de unidades vendidas en el día
    private Double ingresos;        // subtotal generado por este plato
}