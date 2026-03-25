package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioItemDTO {
    private Long idAlimento;
    private String nombreAlimento;
    private String tipoMedida;
}