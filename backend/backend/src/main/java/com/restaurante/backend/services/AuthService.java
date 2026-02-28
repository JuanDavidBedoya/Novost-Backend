package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.Rol;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.mappers.UsuarioMapper;
import com.restaurante.backend.repositories.RolRepository;
import com.restaurante.backend.repositories.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioMapper usuarioMapper;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository, 
                       PasswordEncoder passwordEncoder, JwtService jwtService, 
                       EmailService emailService, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public void iniciarLogin(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("password:Datos incorrectos"));

        if (!passwordEncoder.matches(request.password(), usuario.getContrasenia())) {
            throw new RuntimeException("password:Datos incorrectos");
        }

        String codigo2FA = String.format("%06d", new Random().nextInt(999999));
        usuario.setCodigoVerificacion(codigo2FA);
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(2));
        usuarioRepository.save(usuario);

        emailService.enviarCorreo(usuario.getEmail(), "Tu código de acceso", 
            "Tu código de verificación es: " + codigo2FA + ". Expirará en 2 minutos.");
    }

    @Transactional
    public AuthResponseDTO verificarCodigo(VerifyCodeDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("codigo:Datos incorrectos"));

        if (usuario.getCodigoVerificacion() == null || !usuario.getCodigoVerificacion().equals(request.codigo())) {
            throw new RuntimeException("codigo:Código de verificación incorrecto o expirado");
        }

        if (usuario.getExpiracionCodigo().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("codigo:Código de verificación incorrecto o expirado");
        }

        usuario.setCodigoVerificacion(null);
        usuario.setExpiracionCodigo(null);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        org.springframework.security.core.userdetails.User userDetails = 
            new org.springframework.security.core.userdetails.User(
                usuario.getEmail(), 
                usuario.getContrasenia(), 
                Collections.singletonList(new SimpleGrantedAuthority(usuario.getRol().getNombre()))
            );

        String token = jwtService.generateToken(userDetails);
        UsuarioResponseDTO dto = usuarioMapper.toUsuarioResponseDTO(usuario);
        
        return new AuthResponseDTO(token, dto);
    }

    @Transactional
    public UsuarioResponseDTO registrar(RegistroUsuarioDTO request) {
        if (usuarioRepository.existsById(request.cedula())) {
            throw new RuntimeException("cedula:Cédula duplicada");
        }
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("email:Email duplicado");
        }

        Rol rolUsuario = rolRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("general:Rol no encontrado"));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setCedula(request.cedula());
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setEmail(request.email());
        nuevoUsuario.setTelefono(request.telefono());
        nuevoUsuario.setContrasenia(passwordEncoder.encode(request.contrasena()));
        nuevoUsuario.setRol(rolUsuario);

        usuarioRepository.save(nuevoUsuario);

        emailService.enviarCorreo(nuevoUsuario.getEmail(), "Bienvenido al Restaurante", 
            "¡Hola " + nuevoUsuario.getNombre() + "! Tu registro ha sido exitoso.");

        return usuarioMapper.toUsuarioResponseDTO(nuevoUsuario);
    }

    @Transactional
    public void solicitarRecuperacionPassword(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("email:Usuario no encontrado"));

        String token = UUID.randomUUID().toString();
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(5)); 
        usuarioRepository.save(usuario);

        // ¡ATENCIÓN! Cambié el puerto a 3000 asumiendo que es donde corre el Frontend (o usa 5173 para Vite)
        String link = "http://localhost:5173/restaurar-password?token=" + token;
        emailService.enviarCorreo(usuario.getEmail(), "Recuperación de contraseña", 
            "Ingresa a este link para restaurar tu contraseña: " + link + "\nEste link expira en 5 minutos.");
    }

    @Transactional
    public void restaurarPassword(String token, String nuevaContrasenia) {
        Usuario usuario = usuarioRepository.findByTokenRecuperacion(token)
                .orElseThrow(() -> new RuntimeException("general:Token inválido"));

        if (usuario.getExpiracionCodigo().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("general:El link de recuperación ha expirado");
        }

        // NUEVA VALIDACIÓN: Que no sea la misma contraseña
        if (passwordEncoder.matches(nuevaContrasenia, usuario.getContrasenia())) {
            throw new RuntimeException("nuevaContrasenia:La nueva contraseña no puede ser igual a la anterior");
        }

        usuario.setContrasenia(passwordEncoder.encode(nuevaContrasenia));
        usuario.setTokenRecuperacion(null);
        usuario.setExpiracionCodigo(null);
        usuarioRepository.save(usuario);
    }
}