package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.repositories.UsuarioRepository;
import com.restaurante.backend.services.AuthService;
import com.restaurante.backend.services.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthService authService, UsuarioRepository usuarioRepository) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO request) {
        authService.iniciarLogin(request);
        return ResponseEntity.ok("Código de verificación enviado al correo");
    }

    @PostMapping("/verificar-login")
    public ResponseEntity<AuthResponseDTO> verifyLogin(@Valid @RequestBody VerifyCodeDTO request) {
        return ResponseEntity.ok(authService.verificarCodigo(request));
    }

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponseDTO> register(
            @Valid @RequestBody RegistroUsuarioDTO request) {
 
        // Verificar antes de registrar si la cédula corresponde a una cuenta desactivada
        // para poder devolver el status HTTP correcto sin duplicar lógica
        boolean esReactivacion = usuarioRepository.findById(request.cedula())
                .map(u -> UsuarioService.CUENTA_DESACTIVADA.equals(u.getTokenRecuperacion()))
                .orElse(false);
 
        UsuarioResponseDTO resultado = authService.registrar(request);
 
        // 202 ACCEPTED indica al frontend que fue reactivación, no registro nuevo
        HttpStatus status = esReactivacion ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(resultado);
    }

    @PostMapping("/recobrar-password")
    public ResponseEntity<String> recoverPassword(@Valid @RequestBody SolicitarRecuperacionDTO request) {
        authService.solicitarRecuperacionPassword(request.email());
        return ResponseEntity.ok("Enlace de recuperación enviado");
    }

    @PostMapping("/resetear-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody RestaurarPasswordDTO request) {
        authService.restaurarPassword(request.token(), request.nuevaContrasenia());
        return ResponseEntity.ok("Contraseña restaurada exitosamente");
    }

    @PostMapping("/registrar-trabajador")
    public ResponseEntity<UsuarioResponseDTO> registerTrabajador(@Valid @RequestBody RegistroUsuarioDTO request) {
        return ResponseEntity.ok(authService.registrarTrabajador(request));
    }
}