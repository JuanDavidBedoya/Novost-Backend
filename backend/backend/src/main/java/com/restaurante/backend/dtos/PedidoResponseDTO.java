package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoResponseDTO {

    private Long idPedido;
    private LocalDate fechaPedido;
    private LocalTime horaPedido;
    private Long idMesa;
    private Integer numeroMesa;
    private Long idReserva; // Nulo si no había reserva en ese momento
    private Double subtotal;
    private Double total;
    private String estadoPedido;
    private String observaciones;
}