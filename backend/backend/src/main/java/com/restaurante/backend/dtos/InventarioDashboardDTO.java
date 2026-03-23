package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioDashboardDTO {

    private List<InventarioResponseDTO> productosStockMinimo;
    private List<InventarioResponseDTO> productosMasUtilizados;
    private ResumenInventarioDTO resumenGeneral;
    private Double consumoTotalDia;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResumenInventarioDTO {
        private Long totalProductos;
        private Long productosPorDebajoMinimo;
        private Double stockTotalKilos;
        private Double stockTotalLitros;
        private Long totalUnidades;
    }
}
