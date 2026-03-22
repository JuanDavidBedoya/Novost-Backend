package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoPedidoResponseDTO {

    private Long idPagoPedido;
    private Long idPedido;
    private Long idReserva;       // null si el pedido no tenía reserva
    private String idPasarela;    // null si fue pago en caja
    private String metodoPago;
    private String estadoPago;
    private Double monto;
    private LocalDateTime fechaPago;
}