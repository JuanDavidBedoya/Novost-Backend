package com.restaurante.backend.dtos;

public record AuthResponseDTO(
    String token,
    UsuarioResponseDTO user
) {}