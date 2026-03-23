package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.repositories.UsuarioRepository;
import com.restaurante.backend.services.AuditService;
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
    private final AuditService auditService;

    public AuthController(AuthService authService, UsuarioRepository usuarioRepository, AuditService auditService) {
        this.authService = authService;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO request) {
        authService.iniciarLogin(request);
        
        // Log de intento de login
        auditService.registrar(AuditService.ACCION_LOGIN, AuditService.ENTIDAD_AUTENTICACION, null, 
            "Intento de login para email: " + request.email());
        
        return ResponseEntity.ok("Código de verificación enviado al correo");
    }

    @PostMapping("/verificar-login")
    public ResponseEntity<AuthResponseDTO> verifyLogin(@Valid @RequestBody VerifyCodeDTO request) {
        AuthResponseDTO response = authService.verificarCodigo(request);
        
        // Log de login exitoso
        auditService.registrar(AuditService.ACCION_LOGIN, AuditService.ENTIDAD_AUTENTICACION, null, 
            "Login exitoso para email: " + request.email());
        
        return ResponseEntity.ok(response);
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
        
        // Log de recuperación de contraseña
        auditService.registrar(AuditService.ACCION_ACTUALIZAR, AuditService.ENTIDAD_AUTENTICACION, null, 
            "Solicitud de recuperación de contraseña para email: " + request.email());
        
        return ResponseEntity.ok("Enlace de recuperación enviado");
    }

    @PostMapping("/resetear-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody RestaurarPasswordDTO request) {
        authService.restaurarPassword(request.token(), request.nuevaContrasenia());
        
        // Log de restauración de contraseña
        auditService.registrar(AuditService.ACCION_ACTUALIZAR, AuditService.ENTIDAD_AUTENTICACION, null, 
            "Restauración de contraseña completada");
        
        return ResponseEntity.ok("Contraseña restaurada exitosamente");
    }

    @PostMapping("/registrar-trabajador")
    public ResponseEntity<UsuarioResponseDTO> registerTrabajador(@Valid @RequestBody RegistroUsuarioDTO request) {
        UsuarioResponseDTO resultado = authService.registrarTrabajador(request);
        
        // Log de registro de trabajador
        auditService.registrar(AuditService.ACCION_CREAR, AuditService.ENTIDAD_USUARIO, null, 
            "Registro de nuevo trabajador: " + request.cedula());
        
        return ResponseEntity.ok(resultado);
    }
}