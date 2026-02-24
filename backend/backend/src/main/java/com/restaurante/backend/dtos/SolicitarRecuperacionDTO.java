package com.restaurante.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRecuperacionDTO(
    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "Formato de correo inválido")
    String email
) {}