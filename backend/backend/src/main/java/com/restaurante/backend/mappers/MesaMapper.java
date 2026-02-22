package com.restaurante.backend.mappers;

import org.springframework.stereotype.Component;

import com.restaurante.backend.dtos.MesaResponseDTO;
import com.restaurante.backend.entities.Mesa;

@Component
public class MesaMapper {
    public MesaResponseDTO toResponseDTO(Mesa mesa) {
        if (mesa == null) return null;
        return new MesaResponseDTO(
            mesa.getIdMesa(),
            mesa.getCapacidad(),
            mesa.getNumeroMesa()
        );
    }
}
