package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.MesaResponseDTO;
import com.restaurante.backend.repositories.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaRepository mesaRepo;
    @GetMapping
    public ResponseEntity<List<MesaResponseDTO>> obtenerMesas() {
        List<MesaResponseDTO> mesas = mesaRepo.findAll()
                .stream()
                .sorted((a, b) -> a.getNumeroMesa().compareTo(b.getNumeroMesa()))
                .map(m -> new MesaResponseDTO(m.getIdMesa(), m.getNumeroMesa(), m.getCapacidad()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(mesas);
    }
}