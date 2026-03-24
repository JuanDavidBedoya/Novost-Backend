package com.restaurante.backend.dtos;

import com.restaurante.backend.entities.Inventario.TipoMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioRequestDTO {

    @NotBlank(message = "El nombre del alimento es requerido")
    private String nombreAlimento;

    @NotNull(message = "El tipo de medida es requerido")
    private TipoMedida tipoMedida;

    @NotNull(message = "El stock actual es requerido")
    @Positive(message = "El stock actual debe ser positivo")
    private Double stockActual;

    @NotNull(message = "El stock mínimo es requerido")
    @Positive(message = "El stock mínimo debe ser positivo")
    private Double stockMinimo;
}
