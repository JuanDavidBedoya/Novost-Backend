package com.restaurante.backend.dtos;

public record UsuarioResponseDTO(
    String cedula,
    String nombre,
    String email,
    String telefono,
    String rol
) {}