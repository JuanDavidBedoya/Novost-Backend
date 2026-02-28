package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.ActualizarPerfilDTO;
import com.restaurante.backend.dtos.CambiarContrasenaDTO;
import com.restaurante.backend.dtos.UsuarioResponseDTO;
import com.restaurante.backend.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // NUEVO ENDPOINT
    @GetMapping("/{cedula}")
    public ResponseEntity<UsuarioResponseDTO> getProfile(@PathVariable String cedula) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(cedula));
    }

    @PutMapping("/{cedula}")
    public ResponseEntity<String> updateProfile(@PathVariable String cedula, @Valid @RequestBody ActualizarPerfilDTO dto) {
        usuarioService.actualizarPerfil(cedula, dto);
        return ResponseEntity.ok("Perfil actualizado");
    }

    @PutMapping("/{cedula}/password")
    public ResponseEntity<String> changePassword(@PathVariable String cedula, @Valid @RequestBody CambiarContrasenaDTO dto) {
        usuarioService.cambiarContrasenia(cedula, dto);
        return ResponseEntity.ok("Contraseña actualizada exitosamente");
    }
}