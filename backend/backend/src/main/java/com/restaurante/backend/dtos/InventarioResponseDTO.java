package com.restaurante.backend.dtos;

import com.restaurante.backend.entities.Inventario.TipoMedida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioResponseDTO {

    private Long idAlimento;
    private String nombreAlimento;
    private TipoMedida tipoMedida;
    private Double stockActual;
    private Double stockMinimo;
    private Double consumoHoy;
    private Double ultimoConsumo;
    private LocalDate fechaActualizacion;
    private Boolean belowMinStock;
}
