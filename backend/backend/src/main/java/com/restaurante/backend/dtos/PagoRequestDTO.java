package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de request para confirmar un pago
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoRequestDTO {

    private Long idReserva;
    private String idPasarela;
    private Double monto;
}
