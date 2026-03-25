package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plato_imagenes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long idImagen;

    @OneToOne
    @JoinColumn(name = "id_plato", nullable = false, unique = true)
    private Plato plato;

    @Column(name = "imagen_url", nullable = false)
    private String imagenUrl;
}