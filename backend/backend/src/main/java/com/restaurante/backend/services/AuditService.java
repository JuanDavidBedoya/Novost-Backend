package com.restaurante.backend.services;

import com.restaurante.backend.entities.AuditLog;
import com.restaurante.backend.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para registrar todas las acciones realizadas en el sistema.
 * Proporciona métodos para registrar logs de auditoría de forma síncrona y asíncrona.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // Constantes para tipos de acciones
    public static final String ACCION_CREAR = "CREAR";
    public static final String ACCION_ACTUALIZAR = "ACTUALIZAR";
    public static final String ACCION_ELIMINAR = "ELIMINAR";
    public static final String ACCION_CONSULTAR = "CONSULTAR";
    public static final String ACCION_LOGIN = "LOGIN";
    public static final String ACCION_LOGOUT = "LOGOUT";
    public static final String ACCION_ERROR = "ERROR";

    // Constantes para tipos de entidades
    public static final String ENTIDAD_USUARIO = "USUARIO";
    public static final String ENTIDAD_RESERVA = "RESERVA";
    public static final String ENTIDAD_PEDIDO = "PEDIDO";
    public static final String ENTIDAD_INVENTARIO = "INVENTARIO";
    public static final String ENTIDAD_MESA = "MESA";
    public static final String ENTIDAD_PLATO = "PLATO";
    public static final String ENTIDAD_PAGO = "PAGO";
    public static final String ENTIDAD_AUTENTICACION = "AUTENTICACION";

    /**
     * Registra un log de auditoría de forma síncrona.
     * Este método bloquea hasta que el log se guarde en la base de datos.
     */
    @Transactional
    public AuditLog registrar(String accion, String tipoEntidad, Long idEntidad, String descripcion) {
        return registrar(accion, tipoEntidad, idEntidad, descripcion, null, null);
    }

    /**
     * Registra un log de auditoría con datos adicionales.
     */
    @Transactional
    public AuditLog registrar(String accion, String tipoEntidad, Long idEntidad, String descripcion, 
                              String datosAnteriores, String datosNuevos) {
        AuditLog auditLog = new AuditLog();
        auditLog.setFechaHora(LocalDateTime.now());
        auditLog.setAccion(accion);
        auditLog.setTipoEntidad(tipoEntidad);
        auditLog.setIdEntidad(idEntidad);
        auditLog.setDescripcion(descripcion);
        auditLog.setDatosAnteriores(datosAnteriores);
        auditLog.setDatosNuevos(datosNuevos);
        auditLog.setExitoso(true);

        // Obtener información del usuario actual desde SecurityContext
        agregarInfoUsuario(auditLog);

        AuditLog guardado = auditLogRepository.save(auditLog);
        log.info("AUDIT: {} - {} - {} - {}", 
                auditLog.getFechaHora(), 
                auditLog.getAccion(), 
                auditLog.getTipoEntidad(), 
                auditLog.getDescripcion());
        
        return guardado;
    }

    /**
     * Registra un log de auditoría de forma asíncrona (no bloqueante).
     * Útil para registrar operaciones que no necesitan esperar a que se guarde el log.
     */
    @Async
    public void registrarAsync(String accion, String tipoEntidad, Long idEntidad, String descripcion) {
        try {
            registrar(accion, tipoEntidad, idEntidad, descripcion);
        } catch (Exception e) {
            log.error("Error al registrar log asíncrono: {}", e.getMessage());
        }
    }

    /**
     * Registra un error en el log de auditoría.
     */
    @Transactional
    public AuditLog registrarError(String accion, String tipoEntidad, Long idEntidad, 
                                   String descripcion, String mensajeError) {
        AuditLog auditLog = new AuditLog();
        auditLog.setFechaHora(LocalDateTime.now());
        auditLog.setAccion(accion);
        auditLog.setTipoEntidad(tipoEntidad);
        auditLog.setIdEntidad(idEntidad);
        auditLog.setDescripcion(descripcion);
        auditLog.setExitoso(false);
        auditLog.setMensajeError(mensajeError);

        agregarInfoUsuario(auditLog);

        return auditLogRepository.save(auditLog);
    }

    /**
     * Registra una acción de HTTP (GET, POST, PUT, DELETE).
     */
    @Transactional
    public AuditLog registrarHttp(String metodoHttp, String endpoint, String accion, 
                                   String tipoEntidad, Long idEntidad, String descripcion) {
        AuditLog auditLog = new AuditLog();
        auditLog.setFechaHora(LocalDateTime.now());
        auditLog.setMetodoHttp(metodoHttp);
        auditLog.setEndpoint(endpoint);
        auditLog.setAccion(accion);
        auditLog.setTipoEntidad(tipoEntidad);
        auditLog.setIdEntidad(idEntidad);
        auditLog.setDescripcion(descripcion);
        auditLog.setExitoso(true);

        agregarInfoUsuario(auditLog);

        return auditLogRepository.save(auditLog);
    }

    /**
     * Extrae la información del usuario actual del SecurityContext.
     */
    private void agregarInfoUsuario(AuditLog auditLog) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                
                // El principal puede ser un objeto Usuario o un String (email)
                Object principal = authentication.getPrincipal();
                
                if (principal instanceof com.restaurante.backend.entities.Usuario) {
                    com.restaurante.backend.entities.Usuario usuario = 
                        (com.restaurante.backend.entities.Usuario) principal;
                    auditLog.setUsuarioCedula(usuario.getCedula());
                    auditLog.setUsuarioEmail(usuario.getEmail());
                    auditLog.setUsuarioRol(usuario.getRol().getNombre());
                } else if (principal instanceof String) {
                    auditLog.setUsuarioEmail((String) principal);
                }
                
                // Obtener roles del authentication
                authentication.getAuthorities().forEach(authority -> {
                    if (auditLog.getUsuarioRol() == null) {
                        auditLog.setUsuarioRol(authority.getAuthority());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener información del usuario: {}", e.getMessage());
        }
    }

    /**
     * Obtiene todos los logs ordenados por fecha descendente.
     */
    public List<AuditLog> obtenerTodos() {
        return auditLogRepository.findAll();
    }

    /**
     * Obtiene logs por ID de entidad.
     */
    public List<AuditLog> obtenerPorEntidad(String tipoEntidad) {
        return auditLogRepository.findByTipoEntidadOrderByFechaHoraDesc(tipoEntidad);
    }

    /**
     * Obtiene logs por usuario.
     */
    public List<AuditLog> obtenerPorUsuario(String cedula) {
        return auditLogRepository.findByUsuarioCedulaOrderByFechaHoraDesc(cedula);
    }

    /**
     * Obtiene logs por rango de fechas.
     */
    public List<AuditLog> obtenerPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return auditLogRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(fechaInicio, fechaFin);
    }

    /**
     * Obtiene logs con paginación y filtros.
     */
    public Page<AuditLog> obtenerConFiltros(String usuarioCedula, String tipoEntidad, String accion,
                                           LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                           Boolean exitoso, Pageable pageable) {
        return auditLogRepository.buscarConFiltros(usuarioCedula, tipoEntidad, accion, 
                                                   fechaInicio, fechaFin, exitoso, pageable);
    }

    /**
     * Obtiene un log por ID.
     */
    public Optional<AuditLog> obtenerPorId(Long id) {
        return auditLogRepository.findById(id);
    }

    /**
     * Elimina un log por ID.
     */
    @Transactional
    public void eliminar(Long id) {
        auditLogRepository.deleteById(id);
    }

    /**
     * Registra automáticamente una acción CRUD.
     * Métood de conveniencia para los controladores.
     */
    public void logCrud(String tipoEntidad, Long idEntidad, String accion, String descripcion) {
        registrar(accion, tipoEntidad, idEntidad, descripcion);
    }

    /**
     * Registra consulta (LECTURA) de datos.
     */
    public void logConsulta(String tipoEntidad, Long idEntidad, String descripcion) {
        registrar(ACCION_CONSULTAR, tipoEntidad, idEntidad, descripcion);
    }

    /**
     * Registra creación de datos.
     */
    public void logCreacion(String tipoEntidad, Long idEntidad, String descripcion) {
        registrar(ACCION_CREAR, tipoEntidad, idEntidad, descripcion);
    }

    /**
     * Registra actualización de datos.
     */
    public void logActualizacion(String tipoEntidad, Long idEntidad, String descripcion, 
                                String datosAnteriores, String datosNuevos) {
        registrar(ACCION_ACTUALIZAR, tipoEntidad, idEntidad, descripcion, datosAnteriores, datosNuevos);
    }

    /**
     * Registra eliminación de datos.
     */
    public void logEliminacion(String tipoEntidad, Long idEntidad, String descripcion) {
        registrar(ACCION_ELIMINAR, tipoEntidad, idEntidad, descripcion);
    }
}
