package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.MesaResponseDTO;
import com.restaurante.backend.repositories.MesaRepository;
import com.restaurante.backend.services.AuditService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mesas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMIN')")
public class MesaController {

    private final MesaRepository mesaRepo;
    private final AuditService auditService;
    
    @GetMapping
    public ResponseEntity<List<MesaResponseDTO>> obtenerMesas() {
        List<MesaResponseDTO> mesas = mesaRepo.findAll()
                .stream()
                .sorted((a, b) -> a.getNumeroMesa().compareTo(b.getNumeroMesa()))
                .map(m -> new MesaResponseDTO(m.getIdMesa(), m.getCapacidad(), m.getNumeroMesa()))
                .collect(Collectors.toList());

        // Log de consulta
        auditService.logConsulta(AuditService.ENTIDAD_MESA, null, 
            "Consulta de todas las mesas");
        
        return ResponseEntity.ok(mesas);
    }
}