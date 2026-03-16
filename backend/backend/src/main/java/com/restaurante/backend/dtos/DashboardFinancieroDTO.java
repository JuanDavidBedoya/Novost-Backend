package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardFinancieroDTO {
    private Double kpiHoy;
    private Double kpiSemana;
    private Double kpiTotal;
    private List<ChartDataDTO> chartHoy;
    private List<ChartDataDTO> chartSemana;
    private List<ChartDataDTO> chartMeses;
}