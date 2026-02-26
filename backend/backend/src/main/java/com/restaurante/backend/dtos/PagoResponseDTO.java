package com.restaurante.backend.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de response para devolver datos de un pago
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoResponseDTO {

    private Long idPago;
    private Long idReserva;
    private String idPasarela;
    private String estadoPago;
    private LocalDateTime fechaPago;
    private Double monto;
}
