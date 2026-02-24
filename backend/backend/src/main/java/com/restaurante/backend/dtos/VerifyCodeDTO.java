package com.restaurante.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record VerifyCodeDTO(
    @NotBlank(message = "El email no puede estar vacío")
    String email,

    @NotBlank(message = "El código no puede estar vacío")
    String codigo
) {}
