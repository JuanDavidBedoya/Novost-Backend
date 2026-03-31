package com.restaurante.backend.dtos;

import lombok.Data;

@Data
public class PropagacionEventoDTO {
    private Long idPlato;
    private double duracionSegundos;   // tiempo entre toggle y detección en el menú
    private String accion;             // "deshabilitar" o "habilitar"
}