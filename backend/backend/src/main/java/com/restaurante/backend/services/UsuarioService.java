package com.restaurante.backend.services;

import com.restaurante.backend.dtos.ActualizarPerfilDTO;
import com.restaurante.backend.dtos.CambiarContrasenaDTO;
import com.restaurante.backend.dtos.UsuarioResponseDTO;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.mappers.UsuarioMapper;
import com.restaurante.backend.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public UsuarioResponseDTO actualizarPerfil(String cedula, ActualizarPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setNombre(dto.nombre());
        usuario.setTelefono(dto.telefono());
        usuarioRepository.save(usuario);
        
        // Retornamos el usuario actualizado
        return usuarioMapper.toUsuarioResponseDTO(usuario);
    }

    @Transactional
    public void cambiarContrasenia(String cedula, CambiarContrasenaDTO dto) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.contrasenaAnterior(), usuario.getContrasenia())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        usuario.setContrasenia(passwordEncoder.encode(dto.contrasenaNueva()));
        usuarioRepository.save(usuario);
    }
}