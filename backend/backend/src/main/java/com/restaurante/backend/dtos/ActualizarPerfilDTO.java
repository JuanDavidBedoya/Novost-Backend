package com.restaurante.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record ActualizarPerfilDTO(
    @NotBlank(message = "El nombre no puede estar vacío")
    String nombre,
    String telefono
) {}