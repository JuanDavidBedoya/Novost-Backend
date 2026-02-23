package com.restaurante.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestaurarPasswordDTO(
    @NotBlank(message = "El token es obligatorio")
    String token,
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "Mínimo 8 caracteres")
    String nuevaContrasenia
) {}