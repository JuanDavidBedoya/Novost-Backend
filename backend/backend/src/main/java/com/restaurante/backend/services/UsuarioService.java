package com.restaurante.backend.services;

import com.restaurante.backend.dtos.ActualizarPerfilDTO;
import com.restaurante.backend.dtos.CambiarContrasenaDTO;
import com.restaurante.backend.dtos.DesactivarCuentaRequestDTO;
import com.restaurante.backend.dtos.UsuarioResponseDTO;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.exceptions.ValidationException;
import com.restaurante.backend.mappers.UsuarioMapper;
import com.restaurante.backend.repositories.UsuarioRepository;

import java.time.Instant;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final EmailService emailService;

    public static final String CUENTA_DESACTIVADA = "CUENTA_DESACTIVADA";

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, UsuarioMapper usuarioMapper, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
        this.emailService = emailService;
    }

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

    @Transactional(readOnly = true)
    public String obtenerCedulaPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("general:Usuario no encontrado"));
        return usuario.getCedula();
    }

    @Transactional
    public void desactivarCuenta(DesactivarCuentaRequestDTO request) {
        String emailLogueado = SecurityContextHolder.getContext()
                .getAuthentication().getName();
 
        Usuario usuario = usuarioRepository.findByEmail(emailLogueado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", emailLogueado));
 
        // 1. Verificar que la cuenta no esté ya desactivada
        if (CUENTA_DESACTIVADA.equals(usuario.getTokenRecuperacion())) {
            throw new ValidationException("cuenta", "Esta cuenta ya está desactivada.");
        }
 
        // 2. Verificar contraseña — si es incorrecta lanzamos ValidationException
        //    para que el frontend lo muestre como error inline en el campo
        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenia())) {
            throw new ValidationException("contrasena", "La contraseña ingresada es incorrecta.");
        }
 
        // 3. Enviar correo de despedida ANTES de anonimizar el email
        //    (después ya no tendremos la dirección real del usuario)
        emailService.enviarConfirmacionBaja(usuario.getEmail(), usuario.getNombre());
 
        // 4. Timestamp único para garantizar que el email anonimizado no colisione
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
 
        // 5. Anonimizar solo el email — la cédula es @Id (PK) y Hibernate no permite modificarla.
        //    El email anonimizado libera esa dirección para futuros registros.
        usuario.setEmail("BAJA_" + usuario.getEmail() + "_" + timestamp);
 
        // 5. Marcar como desactivada e invalidar cualquier código/token activo
        usuario.setTokenRecuperacion(CUENTA_DESACTIVADA);
        usuario.setCodigoVerificacion(null);
        usuario.setExpiracionCodigo(null);
 
        usuarioRepository.save(usuario);
    }
}