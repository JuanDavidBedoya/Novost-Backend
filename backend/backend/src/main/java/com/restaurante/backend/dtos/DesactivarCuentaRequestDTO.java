package com.restaurante.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record DesactivarCuentaRequestDTO(
    @NotBlank(message = "La contraseña es requerida")
    String contrasena
) {}