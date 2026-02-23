package com.restaurante.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioDTO(
    @NotBlank(message = "La cédula es obligatoria")
    String cedula,
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,
    @NotBlank(message = "El email es obligatorio")
    @Email
    String email,
    String telefono,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "Mínimo 8 caracteres")
    String contrasena
) {}