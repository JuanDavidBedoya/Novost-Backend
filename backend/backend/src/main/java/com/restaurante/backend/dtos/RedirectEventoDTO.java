package com.restaurante.backend.dtos;

import lombok.Data;

@Data
public class RedirectEventoDTO {
    private double duracionSegundos;  // tiempo medido en el frontend
    private String origen;            // "menu"
    private String destino;           // "pedidos"
}