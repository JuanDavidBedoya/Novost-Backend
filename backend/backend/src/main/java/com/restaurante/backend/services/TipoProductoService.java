package com.restaurante.backend.services;

import com.restaurante.backend.dtos.TipoProductoDTO;
import com.restaurante.backend.entities.TipoProducto;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.repositories.TipoProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipoProductoService {

    private final TipoProductoRepository tipoProductoRepository;
    private final AuditService auditService;

    @Transactional
    public TipoProductoDTO crearTipo(TipoProductoDTO request) {
        if (tipoProductoRepository.existsByNombreTipo(request.getNombreTipo())) {
            throw new IllegalArgumentException("El tipo de producto ya existe: " + request.getNombreTipo());
        }
        
        TipoProducto tipo = new TipoProducto();
        tipo.setNombreTipo(request.getNombreTipo());
        tipo.setDescripcion(request.getDescripcion());
        tipo.setActivo(true);
        
        TipoProducto saved = tipoProductoRepository.save(tipo);
        
        // Log de auditoría: crear nuevo tipo de producto
        try {
            auditService.registrar(
                AuditService.ACCION_CREAR,
                "TIPO_PRODUCTO",
                saved.getIdTipo(),
                "Creación de nuevo tipo de producto",
                null,
                "nombreTipo: " + saved.getNombreTipo() + ", descripcion: " + saved.getDescripcion()
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para creación de tipo de producto: " + e.getMessage());
        }
        
        return toDTO(saved);
    }

    public List<TipoProductoDTO> obtenerTodos() {
        return tipoProductoRepository.findByActivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TipoProductoDTO obtenerPorId(Long id) {
        TipoProducto tipo = tipoProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de producto no encontrado: " + id));
        return toDTO(tipo);
    }

    @Transactional
    public TipoProductoDTO actualizarTipo(Long id, TipoProductoDTO request) {
        TipoProducto tipo = tipoProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de producto no encontrado: " + id));
        
        String nombreAnterior = tipo.getNombreTipo();
        String descripcionAnterior = tipo.getDescripcion();
        Boolean activoAnterior = tipo.getActivo();
        
        tipo.setNombreTipo(request.getNombreTipo());
        tipo.setDescripcion(request.getDescripcion());
        tipo.setActivo(request.getActivo());
        
        TipoProducto saved = tipoProductoRepository.save(tipo);
        
        // Log de auditoría: actualizar tipo de producto
        try {
            String detalles = String.format("nombre: %s -> %s, descripcion: %s -> %s, activo: %s -> %s",
                nombreAnterior, saved.getNombreTipo(),
                descripcionAnterior, saved.getDescripcion(),
                activoAnterior, saved.getActivo());
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                "TIPO_PRODUCTO",
                id,
                "Actualización de tipo de producto",
                null,
                detalles
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para actualización de tipo: " + e.getMessage());
        }
        
        return toDTO(saved);
    }

    @Transactional
    public void eliminarTipo(Long id) {
        TipoProducto tipo = tipoProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de producto no encontrado: " + id));
        
        String nombreTipo = tipo.getNombreTipo();
        tipoProductoRepository.deleteById(id);
        
        // Log de auditoría: eliminar tipo de producto
        try {
            auditService.registrar(
                AuditService.ACCION_ELIMINAR,
                "TIPO_PRODUCTO",
                id,
                "Eliminación de tipo de producto",
                null,
                "nombreTipo: " + nombreTipo
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para eliminación de tipo: " + e.getMessage());
        }
    }

    private TipoProductoDTO toDTO(TipoProducto entity) {
        TipoProductoDTO dto = new TipoProductoDTO();
        dto.setIdTipo(entity.getIdTipo());
        dto.setNombreTipo(entity.getNombreTipo());
        dto.setDescripcion(entity.getDescripcion());
        dto.setActivo(entity.getActivo());
        return dto;
    }
}