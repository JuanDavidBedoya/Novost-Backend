package com.restaurante.backend.controllers;

import com.restaurante.backend.entities.AuditLog;
import com.restaurante.backend.services.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador para consultar los logs de auditoría.
 * Solo accesible por administradores.
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    /**
     * GET /audit/todos
     * Obtiene todos los logs de auditoría.
     */
    @GetMapping("/todos")
    public ResponseEntity<List<AuditLog>> obtenerTodos() {
        return ResponseEntity.ok(auditService.obtenerTodos());
    }

    /**
     * GET /audit/{id}
     * Obtiene un log por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> obtenerPorId(@PathVariable Long id) {
        return auditService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /audit/por-entidad/{tipoEntidad}
     * Obtiene logs por tipo de entidad.
     */
    @GetMapping("/por-entidad/{tipoEntidad}")
    public ResponseEntity<List<AuditLog>> obtenerPorEntidad(@PathVariable String tipoEntidad) {
        return ResponseEntity.ok(auditService.obtenerPorEntidad(tipoEntidad));
    }

    /**
     * GET /audit/por-usuario/{cedula}
     * Obtiene logs por usuario.
     */
    @GetMapping("/por-usuario/{cedula}")
    public ResponseEntity<List<AuditLog>> obtenerPorUsuario(@PathVariable String cedula) {
        return ResponseEntity.ok(auditService.obtenerPorUsuario(cedula));
    }

    /**
     * GET /audit/por-rango-fechas
     * Obtiene logs por rango de fechas.
     */
    @GetMapping("/por-rango-fechas")
    public ResponseEntity<List<AuditLog>> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(auditService.obtenerPorRangoFechas(fechaInicio, fechaFin));
    }

    /**
     * GET /audit/paginacion
     * Obtiene logs con paginación y filtros.
     */
    @GetMapping("/paginacion")
    public ResponseEntity<Page<AuditLog>> obtenerConPaginacion(
            @RequestParam(required = false) String usuarioCedula,
            @RequestParam(required = false) String tipoEntidad,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) Boolean exitoso,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamaño,
            @RequestParam(defaultValue = "fechaHora") String ordenPor,
            @RequestParam(defaultValue = "desc") String direccion) {
        
        Sort.Direction direction = "asc".equalsIgnoreCase(direccion) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(pagina, tamaño, Sort.by(direction, ordenPor));
        
        Page<AuditLog> logs = auditService.obtenerConFiltros(
                usuarioCedula, tipoEntidad, accion, fechaInicio, fechaFin, exitoso, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * DELETE /audit/{id}
     * Elimina un log por ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarLog(@PathVariable Long id) {
        auditService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
