package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CierreCajaDTO {
    private LocalDate fecha;
    private Double totalDia;
    private Double totalCaja;
    private Double totalLinea;
    private Integer cantidadPedidos;
    private Integer cantidadCaja;
    private Integer cantidadLinea;
}