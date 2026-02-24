package com.restaurante.backend.mappers;

import com.restaurante.backend.dtos.UsuarioResponseDTO;
import com.restaurante.backend.entities.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponseDTO toUsuarioResponseDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        String nombreRol = (usuario.getRol() != null) ? usuario.getRol().getNombre() : "SIN_ROL";

        return new UsuarioResponseDTO(
                usuario.getCedula(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                nombreRol
        );
    }
}