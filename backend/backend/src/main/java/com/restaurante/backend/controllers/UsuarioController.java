package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.ActualizarPerfilDTO;
import com.restaurante.backend.dtos.CambiarContrasenaDTO;
import com.restaurante.backend.dtos.DesactivarCuentaRequestDTO;
import com.restaurante.backend.dtos.UsuarioResponseDTO;
import com.restaurante.backend.services.AuditService;
import com.restaurante.backend.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditService auditService;

    public UsuarioController(UsuarioService usuarioService, AuditService auditService) {
        this.usuarioService = usuarioService;
        this.auditService = auditService;
    }

    @GetMapping("/{cedula}")
    public ResponseEntity<UsuarioResponseDTO> getProfile(@PathVariable String cedula) {
        UsuarioResponseDTO usuario = usuarioService.obtenerUsuario(cedula);
        
        // Log de consulta
        auditService.logConsulta(AuditService.ENTIDAD_USUARIO, null, 
            "Consulta de perfil de usuario: " + cedula);
        
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/{cedula}")
    public ResponseEntity<String> updateProfile(@PathVariable String cedula, @Valid @RequestBody ActualizarPerfilDTO dto) {
        usuarioService.actualizarPerfil(cedula, dto);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_USUARIO, null, 
            "Actualización de perfil de usuario: " + cedula, null, 
            "nombre: " + dto.nombre() + ", telefono: " + dto.telefono());
        
        return ResponseEntity.ok("Perfil actualizado");
    }

    @PutMapping("/{cedula}/password")
    public ResponseEntity<String> changePassword(@PathVariable String cedula, @Valid @RequestBody CambiarContrasenaDTO dto) {
        usuarioService.cambiarContrasenia(cedula, dto);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_USUARIO, null, 
            "Cambio de contraseña de usuario: " + cedula, null, null);
        
        return ResponseEntity.ok("Contraseña actualizada exitosamente");
    }

    @PatchMapping("/desactivar")
    public ResponseEntity<String> desactivarCuenta(
            @Valid @RequestBody DesactivarCuentaRequestDTO request) {
        usuarioService.desactivarCuenta(request);
        
        // Log de eliminación - No tenemos la cédula del usuario que se suicida directamente
        // así que registramos que un usuario desactivó su propia cuenta
        auditService.logEliminacion(AuditService.ENTIDAD_USUARIO, null, 
            "Un usuario solicitó la desactivación de su cuenta");
        
        return ResponseEntity.ok("Tu cuenta ha sido desactivada permanentemente.");
    }
}