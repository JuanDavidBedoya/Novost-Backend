package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardFinancieroDTO {

    // ── KPIs del Día ─────────────────────────────────────────────────────────
    private Double kpiHoy;
    private Double kpiHoyCaja;
    private Double kpiHoyLinea;

    // ── KPIs de la Semana ────────────────────────────────────────────────────
    private Double kpiSemana;
    private Double kpiSemanaCaja;
    private Double kpiSemanaLinea;

    // ── KPIs del Mes ─────────────────────────────────────────────────────────
    private Double kpiMes;
    private Double kpiMesCaja;
    private Double kpiMesLinea;

    // ── Gráficas ─────────────────────────────────────────────────────────────
    private List<ChartDataDTO> chartHoy;     // Ingresos por hora del día
    private List<ChartDataDTO> chartSemana;  // Ingresos por día de la semana
    private List<ChartDataDTO> chartMes;     // Ingresos por día del mes
}