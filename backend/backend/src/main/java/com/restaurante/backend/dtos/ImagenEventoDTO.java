package com.restaurante.backend.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ImagenEventoDTO {
    private String idPlato;
    private String imagenUrl;
    private String navegador;
    private String dispositivo;
    // "exito" o "fallo"
    private String resultado;
}