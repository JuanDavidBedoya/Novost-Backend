package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataDTO {
    private String label; // "12:00", "Lun", "Ene"
    private Double total; // 150000.0
}