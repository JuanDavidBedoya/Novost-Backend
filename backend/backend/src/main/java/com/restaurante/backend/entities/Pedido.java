package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "pedidos")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDate fechaPedido;

    @Column(name = "hora_pedido", nullable = false)
    private LocalTime horaPedido;

    @ManyToOne
    @JoinColumn(name = "id_mesa", nullable = false)
    private Mesa mesa;

    @ManyToOne
    @JoinColumn(name = "id_reserva", nullable = true)
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = true)
    private Usuario usuario;

    @Column(name = "subtotal", nullable = false)
    private Double subtotal;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "observaciones", nullable = true)
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "id_estado_pedido", nullable = false)
    private EstadoPedido estadoPedido; 
}