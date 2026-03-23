package com.restaurante.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad para almacenar logs de auditoría de todas las acciones realizadas en el sistema.
 * Registra: fecha, hora, qué acción se realizó, quién la realizó y detalles adicionales.
 */
@Entity
@Table(name = "audit_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "tipo_entidad", nullable = false, length = 50)
    private String tipoEntidad;

    @Column(name = "id_entidad")
    private Long idEntidad;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "usuario_cedula", length = 20)
    private String usuarioCedula;

    @Column(name = "usuario_email", length = 150)
    private String usuarioEmail;

    @Column(name = "usuario_rol", length = 20)
    private String usuarioRol;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(name = "endpoint", length = 200)
    private String endpoint;

    @Column(name = "ip_cliente", length = 50)
    private String ipCliente;

    @Column(name = "datos_anteriores", length = 1000)
    private String datosAnteriores;

    @Column(name = "datos_nuevos", length = 1000)
    private String datosNuevos;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso = true;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    /**
     * Constructor para crear un log rápidamente
     */
    public AuditLog(String accion, String tipoEntidad, Long idEntidad, String descripcion) {
        this.fechaHora = LocalDateTime.now();
        this.accion = accion;
        this.tipoEntidad = tipoEntidad;
        this.idEntidad = idEntidad;
        this.descripcion = descripcion;
        this.exitoso = true;
    }
}
