package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardPlatosDTO {

    // Evolución de unidades vendidas por hora del día
    private List<ChartDataDTO> ventasPorHora;

    // Todos los platos vendidos hoy con su porcentaje (para tabla de rendimiento)
    private List<PlatoVentaDTO> platosDia;

    // Distribución por categoría (para pie/bar chart de categorías)
    private List<CategoriaVentaDTO> categorias;

    // Total de unidades vendidas hoy (útil para el frontend)
    private Long totalUnidadesHoy;
}