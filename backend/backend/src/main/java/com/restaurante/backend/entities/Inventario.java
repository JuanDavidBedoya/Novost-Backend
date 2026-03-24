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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_medida", nullable = false, length = 20)
    private TipoMedida tipoMedida;

    @Column(name = "stock_actual", nullable = false)
    private Double stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Double stockMinimo;

    @Column(name = "consumo_hoy", nullable = false)
    private Double consumoHoy;

    @Column(name = "ultimo_consumo", nullable = true)
    private Double ultimoConsumo;

    @Column(name = "fecha_actualizacion")
    private LocalDate fechaActualizacion;

    public enum TipoMedida {
        KILO,
        LITRO,
        UNIDAD
    }
}