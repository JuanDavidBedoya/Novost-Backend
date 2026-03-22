package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "inventario")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alimento")
    private Long idAlimento;

    @Column(name = "nombre_alimento", nullable = false, length = 100)
    private String nombreAlimento;

    @Column(name = "medida", nullable = false, length = 50)
    private String medida;

    @Column(name = "stock_actual", nullable = false)
    private Double stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Double stockMinimo;

    @Column(name = "fecha_actualizacion")
    private LocalDate fechaActualizacion;
}