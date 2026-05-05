package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntradaCompraRequestDTO {
    private Long idAlimento;
    private Double cantidad;
    private LocalDate fechaVencimiento;
    private String lote;
    private Double precioCompra;
    private String proveedor;
    private String cedulaTrabajador;
}