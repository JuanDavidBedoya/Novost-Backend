package com.restaurante.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaDTO(
    @NotBlank(message = "La contraseña actual es obligatoria")
    String contrasenaAnterior,
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8)
    String contrasenaNueva
) {}
