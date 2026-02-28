package com.restaurante.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarPerfilDTO(
    @NotBlank(message = "El nombre no puede estar vacío")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras")
    String nombre,

    @NotBlank(message = "El teléfono no puede estar vacío")
    @Pattern(regexp = "^3\\d{9}$", message = "El teléfono debe tener exactamente 10 números y comenzar por 3")
    String telefono
) {}