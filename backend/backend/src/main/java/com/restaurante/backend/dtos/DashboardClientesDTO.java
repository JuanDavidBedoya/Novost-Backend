package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardClientesDTO {

    // KPIs
    private Long pedidosHoy;
    private Long pedidosSemana;
    private Long pedidosMes;
    private Long reservasHoy;
    private Long reservasSemana;
    private Long reservasMes;

    // Gráficas — pedidos y reservas agrupados por día de la semana actual
    private List<ChartDataDTO> chartPedidosSemana;
    private List<ChartDataDTO> chartReservasSemana;

    // Distribución de reservas por estado en la semana actual
    private List<ChartDataDTO> reservasPorEstado;
}