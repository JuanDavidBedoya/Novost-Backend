package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MesaResponseDTO {

    private Long idMesa;
    private Integer capacidad;
    private Integer numeroMesa;
}
