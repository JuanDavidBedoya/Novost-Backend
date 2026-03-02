package com.restaurante.backend.services;

import com.restaurante.backend.dtos.ActualizarPerfilDTO;
import com.restaurante.backend.dtos.CambiarContrasenaDTO;
import com.restaurante.backend.dtos.UsuarioResponseDTO;
import com.restaurante.backend.entities.Rol;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.mappers.UsuarioMapper;
import com.restaurante.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Rol rol;

    @BeforeEach
    void setUp() {
        rol = new Rol();
        rol.setIdRol(1L);
        rol.setNombre("USUARIO");

        usuario = new Usuario();
        usuario.setCedula("12345678");
        usuario.setNombre("Juan Perez");
        usuario.setEmail("juan@example.com");
        usuario.setTelefono("3001234567");
        usuario.setContrasenia("encodedPassword");
        usuario.setRol(rol);
    }

    @Test
    void obtenerUsuario_UsuarioNoEncontrado_ThrowsException() {
        String cedula = "99999999";
        when(usuarioRepository.findById(cedula)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> usuarioService.obtenerUsuario(cedula));

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void obtenerUsuario_UsuarioEncontrado_ReturnsDTO() {
        UsuarioResponseDTO usuarioResponse = new UsuarioResponseDTO(
            "12345678", "Juan Perez", "juan@example.com", "3001234567", "USUARIO"
        );
        
        when(usuarioRepository.findById("12345678")).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(usuarioResponse);

        UsuarioResponseDTO result = usuarioService.obtenerUsuario("12345678");

        assertNotNull(result);
        assertEquals("12345678", result.cedula());
        assertEquals("Juan Perez", result.nombre());
    }

    @Test
    void actualizarPerfil_UsuarioNoEncontrado_ThrowsException() {
        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan Actualizado", "3009999999");
        when(usuarioRepository.findById("99999999")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> usuarioService.actualizarPerfil("99999999", dto));

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void actualizarPerfil_ActualizacionExitosa_ReturnsDTO() {
        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan Actualizado", "3009999999");
        UsuarioResponseDTO usuarioResponse = new UsuarioResponseDTO(
            "12345678", "Juan Actualizado", "juan@example.com", "3009999999", "USUARIO"
        );
        
        when(usuarioRepository.findById("12345678")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(usuarioResponse);

        UsuarioResponseDTO result = usuarioService.actualizarPerfil("12345678", dto);

        assertNotNull(result);
        assertEquals("Juan Actualizado", result.nombre());
        assertEquals("3009999999", result.telefono());
        verify(usuarioRepository).save(argThat(u -> 
            u.getNombre().equals("Juan Actualizado") && 
            u.getTelefono().equals("3009999999")
        ));
    }

    @Test
    void cambiarContrasenia_UsuarioNoEncontrado_ThrowsException() {
        // Usar los nombres de campo correctos: contrasenaAnterior y contrasenaNueva
        CambiarContrasenaDTO dto = new CambiarContrasenaDTO("oldPassword", "newPassword");
        when(usuarioRepository.findById("99999999")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> usuarioService.cambiarContrasenia("99999999", dto));

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void cambiarContrasenia_ContrasenaAnteriorIncorrecta_ThrowsException() {
        CambiarContrasenaDTO dto = new CambiarContrasenaDTO("wrongPassword", "newPassword");
        
        when(usuarioRepository.findById("12345678")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(dto.contrasenaAnterior(), usuario.getContrasenia())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> usuarioService.cambiarContrasenia("12345678", dto));

        assertTrue(exception.getMessage().contains("contraseña actual es incorrecta"));
    }

    @Test
    void cambiarContrasenia_MismaContrasena_ThrowsException() {
        CambiarContrasenaDTO dto = new CambiarContrasenaDTO("oldPassword", "samePassword");
        
        when(usuarioRepository.findById("12345678")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(dto.contrasenaAnterior(), usuario.getContrasenia())).thenReturn(true);
        when(passwordEncoder.matches(dto.contrasenaNueva(), usuario.getContrasenia())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> usuarioService.cambiarContrasenia("12345678", dto));

        assertTrue(exception.getMessage().contains("no puede ser igual a la anterior"));
    }

    @Test
    void cambiarContrasenia_CambioExitoso() {
        CambiarContrasenaDTO dto = new CambiarContrasenaDTO("oldPassword", "newPassword");
        
        when(usuarioRepository.findById("12345678")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(dto.contrasenaAnterior(), usuario.getContrasenia())).thenReturn(true);
        when(passwordEncoder.matches(dto.contrasenaNueva(), usuario.getContrasenia())).thenReturn(false);
        when(passwordEncoder.encode(dto.contrasenaNueva())).thenReturn("encodedNewPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        assertDoesNotThrow(() -> usuarioService.cambiarContrasenia("12345678", dto));

        verify(usuarioRepository).save(argThat(u -> 
            u.getContrasenia().equals("encodedNewPassword")
        ));
    }

    @Test
    void obtenerCedulaPorEmail_UsuarioNoEncontrado_ThrowsException() {
        String email = "noexiste@example.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> usuarioService.obtenerCedulaPorEmail(email));

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void obtenerCedulaPorEmail_UsuarioEncontrado_ReturnsCedula() {
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));

        String cedula = usuarioService.obtenerCedulaPorEmail("juan@example.com");

        assertEquals("12345678", cedula);
    }
}
