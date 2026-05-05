package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "producto_individual")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoIndividual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alimento", nullable = false)
    private Inventario inventario;

    @Column(name = "cantidad", nullable = false)
    private Double cantidad;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "lote", length = 50)
    private String lote;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso = LocalDate.now();

    @Column(name = "precio_compra", nullable = false)
    private Double precioCompra;

    @Column(name = "proveedor", length = 100)
    private String proveedor;

    @Column(name = "estado", length = 20)
    private String estado = "DISPONIBLE";

    @Column(name = "cedula_trabajador", length = 20)
    private String cedulaTrabajador;

    public enum EstadoProducto {
        DISPONIBLE, VENCIDO, AGOTADO
    }
}