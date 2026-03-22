package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago_pedido")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_pedido")
    private Long idPagoPedido;

    @OneToOne
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "id_pasarela", nullable = true) // null si pago en caja
    private String idPasarela;

    @Column(name = "metodo_pago", nullable = false) // "CAJA" o "LINEA"
    private String metodoPago;

    @Column(name = "estado_pago", nullable = false) // "PAGADO", "PENDIENTE"
    private String estadoPago;

    @Column(name = "monto", nullable = false)
    private Double monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;
}