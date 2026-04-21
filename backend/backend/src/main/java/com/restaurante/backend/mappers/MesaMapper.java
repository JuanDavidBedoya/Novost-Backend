package com.restaurante.backend.mappers;

import org.springframework.stereotype.Component;

import com.restaurante.backend.dtos.MesaResponseDTO;
import com.restaurante.backend.entities.Mesa;

// Mapper para convertir entidad Mesa a DTO de respuesta

@Component
public class MesaMapper {

    // Método toResponseDTO: transforma entidad Mesa en MesaResponseDTO con sus atributos principales
    public MesaResponseDTO toResponseDTO(Mesa mesa) {
        if (mesa == null) return null;
        return new MesaResponseDTO(
            mesa.getIdMesa(),
            mesa.getCapacidad(),
            mesa.getNumeroMesa()
        );
    }
}
