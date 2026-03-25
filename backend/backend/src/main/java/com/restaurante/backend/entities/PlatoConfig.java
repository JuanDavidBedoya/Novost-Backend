package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plato_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_config")
    private Long idConfig;

    @OneToOne
    @JoinColumn(name = "id_plato", nullable = false, unique = true)
    private Plato plato;

    // Estado controlado por el admin, independiente del stock
    @Column(name = "habilitado_admin", nullable = false)
    private Boolean habilitadoAdmin = true;
}