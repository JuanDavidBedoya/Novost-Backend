package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de request para confirmar el pago de una reserva
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaConfirmarPagoRequestDTO {

    private String idPasarela;
    private Double monto;
}
