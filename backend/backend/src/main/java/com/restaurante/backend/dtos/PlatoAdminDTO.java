package com.restaurante.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoAdminDTO {
    private Long idPlato;
    private String nombrePlato;
    private String descripcion;
    private Double precioPlato;
    private String categoriaNombre;
    private String imagenUrl;
    private Boolean disponibleStock;   // calculado por stock
    private Boolean habilitadoAdmin;   // controlado por el admin
}