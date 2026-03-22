package com.restaurante.backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoRequestDTO {

    @NotNull(message = "El ID de la mesa es obligatorio")
    private Long idMesa;

    private String observaciones; // Puede ser nulo, tal como solicitaste

    @NotEmpty(message = "El pedido debe contener al menos un plato")
    @Valid
    private List<PedidoDetalleRequestDTO> detalles;

    @NotNull(message = "El método de pago es obligatorio")
    private String metodoPago;
}