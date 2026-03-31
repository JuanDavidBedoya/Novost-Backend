package com.restaurante.backend.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PedidoFalloEventoDTO {
    private String tipoError;  // CARRITO_VACIO, MESA_NO_SELECCIONADA, ERROR_SERVIDOR
    private String motivo;     // Mensaje exacto que vio el usuario
}