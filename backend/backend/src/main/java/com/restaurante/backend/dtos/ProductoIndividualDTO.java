package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoIndividualDTO {
    private Long idProducto;
    private Long idAlimento;
    private String nombreAlimento;
    private Double cantidad;
    private String tipoMedida;
    private LocalDate fechaVencimiento;
    private String lote;
    private LocalDate fechaIngreso;
    private Double precioCompra;
    private String proveedor;
    private String estado;
    private Boolean proximoAVencer;
    private Long diasParaVencer;
    private String cedulaTrabajador;
}