package com.restaurante.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioDTO(
    @NotBlank(message = "La cédula es requerida")
    String cedula,

    @NotBlank(message = "El nombre completo es requerido")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras")
    String nombre,

    @NotBlank(message = "El email es requerido")
    @Email(message = "Formato de email inválido")
    String email,

    @NotBlank(message = "El teléfono es requerido")
    @Pattern(
        regexp = "^3\\d{9}$",
        message = "El teléfono debe tener exactamente 10 números y comenzar por 3"
    )
    String telefono,

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String contrasena
) {}