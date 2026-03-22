package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plato_detalle")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plato_detalle")
    private Long idPlatoDetalle;

    @ManyToOne
    @JoinColumn(name = "id_plato", nullable = false)
    private Plato plato;

    @ManyToOne
    @JoinColumn(name = "id_alimento", nullable = false)
    private Inventario inventario;

    @Column(name = "cantidad_necesaria", nullable = false)
    private Double cantidadNecesaria;
}