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

    // NUEVO MÉTODO PARA OBTENER LOS DATOS
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerUsuario(String cedula) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new RuntimeException("general:Usuario no encontrado"));
        return usuarioMapper.toUsuarioResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO actualizarPerfil(String cedula, ActualizarPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new RuntimeException("general:Usuario no encontrado"));
        
        usuario.setNombre(dto.nombre());
        usuario.setTelefono(dto.telefono());
        usuarioRepository.save(usuario);
        
        return usuarioMapper.toUsuarioResponseDTO(usuario);
    }

    @Transactional
    public void cambiarContrasenia(String cedula, CambiarContrasenaDTO dto) {
        Usuario usuario = usuarioRepository.findById(cedula)
                .orElseThrow(() -> new RuntimeException("general:Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.contrasenaAnterior(), usuario.getContrasenia())) {
            throw new RuntimeException("contrasenaAnterior:La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(dto.contrasenaNueva(), usuario.getContrasenia())) {
            throw new RuntimeException("contrasenaNueva:La nueva contraseña no puede ser igual a la anterior");
        }

        usuario.setContrasenia(passwordEncoder.encode(dto.contrasenaNueva()));
        usuarioRepository.save(usuario);
    }
}