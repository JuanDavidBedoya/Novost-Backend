package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estados_reserva")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstadoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_reserva")
    private Long idEstadoReserva;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
}