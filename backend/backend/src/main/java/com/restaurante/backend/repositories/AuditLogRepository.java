package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Buscar logs por usuario
    List<AuditLog> findByUsuarioCedulaOrderByFechaHoraDesc(String cedula);

    // Buscar logs por tipo de entidad
    List<AuditLog> findByTipoEntidadOrderByFechaHoraDesc(String tipoEntidad);

    // Buscar logs por acción
    List<AuditLog> findByAccionOrderByFechaHoraDesc(String accion);

    // Buscar logs por rango de fechas
    List<AuditLog> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Buscar logs por usuario y tipo de entidad
    List<AuditLog> findByUsuarioCedulaAndTipoEntidadOrderByFechaHoraDesc(String cedula, String tipoEntidad);

    // Buscar logs exitosos o fallidos
    List<AuditLog> findByExitosoOrderByFechaHoraDesc(Boolean exitoso);

    // Buscar logs por endpoint
    List<AuditLog> findByEndpointOrderByFechaHoraDesc(String endpoint);

    // Consulta con filtros múltiples para paginación
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:usuarioCedula IS NULL OR a.usuarioCedula = :usuarioCedula) AND " +
           "(:tipoEntidad IS NULL OR a.tipoEntidad = :tipoEntidad) AND " +
           "(:accion IS NULL OR a.accion = :accion) AND " +
           "(:fechaInicio IS NULL OR a.fechaHora >= :fechaInicio) AND " +
           "(:fechaFin IS NULL OR a.fechaHora <= :fechaFin) AND " +
           "(:exitoso IS NULL OR a.exitoso = :exitoso) " +
           "ORDER BY a.fechaHora DESC")
    Page<AuditLog> buscarConFiltros(
            @Param("usuarioCedula") String usuarioCedula,
            @Param("tipoEntidad") String tipoEntidad,
            @Param("accion") String accion,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("exitoso") Boolean exitoso,
            Pageable pageable);

    // Contar acciones por tipo en un rango de fechas
    @Query("SELECT a.accion, COUNT(a) FROM AuditLog a WHERE a.fechaHora BETWEEN :fechaInicio AND :fechaFin GROUP BY a.accion")
    List<Object[]> contarAccionesPorTipo(@Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);
}
