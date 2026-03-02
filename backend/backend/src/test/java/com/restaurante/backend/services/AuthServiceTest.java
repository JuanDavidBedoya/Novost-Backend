package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.Rol;
import com.restaurante.backend.entities.Usuario;
import com.restaurante.backend.mappers.UsuarioMapper;
import com.restaurante.backend.repositories.RolRepository;
import com.restaurante.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;
    private Rol rolUsuario;
    private LoginRequestDTO loginRequest;
    private RegistroUsuarioDTO registroRequest;

    @BeforeEach
    void setUp() {
        rolUsuario = new Rol();
        rolUsuario.setIdRol(1L);
        rolUsuario.setNombre("USUARIO");

        usuario = new Usuario();
        usuario.setCedula("12345678");
        usuario.setNombre("Juan Perez");
        usuario.setEmail("juan@example.com");
        usuario.setTelefono("3001234567");
        usuario.setContrasenia("encodedPassword");
        usuario.setRol(rolUsuario);

        loginRequest = new LoginRequestDTO("juan@example.com", "password123");

        registroRequest = new RegistroUsuarioDTO(
            "12345678",
            "Juan Perez",
            "juan@example.com",
            "3001234567",
            "password123"
        );
    }

    @Test
    void iniciarLogin_UsuarioNoExiste_ThrowsException() {
        when(usuarioRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.iniciarLogin(loginRequest));

        assertTrue(exception.getMessage().contains("Datos incorrectos"));
        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void iniciarLogin_PasswordIncorrecto_ThrowsException() {
        when(usuarioRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(loginRequest.password(), usuario.getContrasenia())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.iniciarLogin(loginRequest));

        assertTrue(exception.getMessage().contains("Datos incorrectos"));
        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    void iniciarLogin_LoginExitoso_EnviaCodigo2FA() {
        when(usuarioRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(loginRequest.password(), usuario.getContrasenia())).thenReturn(true);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        assertDoesNotThrow(() -> authService.iniciarLogin(loginRequest));

        verify(usuarioRepository).save(argThat(u -> 
            u.getCodigoVerificacion() != null && 
            u.getExpiracionCodigo() != null
        ));
        verify(emailService).enviarCorreo(eq("juan@example.com"), anyString(), anyString());
    }

    @Test
    void verificarCodigo_UsuarioNoExiste_ThrowsException() {
        VerifyCodeDTO verifyRequest = new VerifyCodeDTO("juan@example.com", "123456");
        when(usuarioRepository.findByEmail(verifyRequest.email())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.verificarCodigo(verifyRequest));

        assertTrue(exception.getMessage().contains("Datos incorrectos"));
    }

    @Test
    void verificarCodigo_CodigoIncorrecto_ThrowsException() {
        VerifyCodeDTO verifyRequest = new VerifyCodeDTO("juan@example.com", "000000");
        usuario.setCodigoVerificacion("123456");
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(5));
        
        when(usuarioRepository.findByEmail(verifyRequest.email())).thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.verificarCodigo(verifyRequest));

        assertTrue(exception.getMessage().contains("Código de verificación incorrecto"));
    }

    @Test
    void verificarCodigo_CodigoExpirado_ThrowsException() {
        VerifyCodeDTO verifyRequest = new VerifyCodeDTO("juan@example.com", "123456");
        usuario.setCodigoVerificacion("123456");
        usuario.setExpiracionCodigo(LocalDateTime.now().minusMinutes(1));
        
        when(usuarioRepository.findByEmail(verifyRequest.email())).thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.verificarCodigo(verifyRequest));

        assertTrue(exception.getMessage().contains("expirado"));
    }

    @Test
    void verificarCodigo_CodigoValido_RetornaToken() {
        VerifyCodeDTO verifyRequest = new VerifyCodeDTO("juan@example.com", "123456");
        usuario.setCodigoVerificacion("123456");
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(5));
        
        UsuarioResponseDTO usuarioResponse = new UsuarioResponseDTO(
            "12345678", "Juan Perez", "juan@example.com", "3001234567", "USUARIO"
        );
        
        when(usuarioRepository.findByEmail(verifyRequest.email())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(usuarioResponse);

        AuthResponseDTO response = authService.verificarCodigo(verifyRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals(usuarioResponse, response.user());
        verify(usuarioRepository).save(argThat(u -> 
            u.getCodigoVerificacion() == null && 
            u.getExpiracionCodigo() == null
        ));
    }

    @Test
    void registrar_CedulaDuplicada_ThrowsException() {
        when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.registrar(registroRequest));

        assertTrue(exception.getMessage().contains("Cédula duplicada"));
    }

    @Test
    void registrar_EmailDuplicado_ThrowsException() {
        when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(false);
        when(usuarioRepository.findByEmail(registroRequest.email())).thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.registrar(registroRequest));

        assertTrue(exception.getMessage().contains("Email duplicado"));
    }

    @Test
    void registrar_RolNoEncontrado_ThrowsException() {
        when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(false);
        when(usuarioRepository.findByEmail(registroRequest.email())).thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.registrar(registroRequest));

        assertTrue(exception.getMessage().contains("Rol no encontrado"));
    }

    @Test
    void registrar_RegistroExitoso_ReturnsUsuario() {
        UsuarioResponseDTO usuarioResponse = new UsuarioResponseDTO(
            "12345678", "Juan Perez", "juan@example.com", "3001234567", "USUARIO"
        );
        
        when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(false);
        when(usuarioRepository.findByEmail(registroRequest.email())).thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolUsuario));
        when(passwordEncoder.encode(registroRequest.contrasena())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUsuarioResponseDTO(usuario)).thenReturn(usuarioResponse);

        UsuarioResponseDTO result = authService.registrar(registroRequest);

        assertNotNull(result);
        assertEquals("12345678", result.cedula());
        verify(emailService).enviarCorreo(eq("juan@example.com"), anyString(), anyString());
    }

    @Test
    void registrarTrabajador_RegistroExitoso_ReturnsUsuario() {
        Rol rolTrabajador = new Rol();
        rolTrabajador.setIdRol(2L);
        rolTrabajador.setNombre("TRABAJADOR");
        
        Usuario usuarioConRolTrabajador = new Usuario();
        usuarioConRolTrabajador.setCedula("12345678");
        usuarioConRolTrabajador.setNombre("Juan Perez");
        usuarioConRolTrabajador.setEmail("juan@example.com");
        usuarioConRolTrabajador.setTelefono("3001234567");
        usuarioConRolTrabajador.setContrasenia("encodedPassword");
        usuarioConRolTrabajador.setRol(rolTrabajador);
        
        UsuarioResponseDTO usuarioResponse = new UsuarioResponseDTO(
            "12345678", "Juan Perez", "juan@example.com", "3001234567", "TRABAJADOR"
        );
        
        when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(false);
        when(usuarioRepository.findByEmail(registroRequest.email())).thenReturn(Optional.empty());
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolTrabajador));
        when(passwordEncoder.encode(registroRequest.contrasena())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioConRolTrabajador);
        when(usuarioMapper.toUsuarioResponseDTO(any(Usuario.class))).thenReturn(usuarioResponse);

        UsuarioResponseDTO result = authService.registrarTrabajador(registroRequest);

        assertNotNull(result);
        verify(emailService).enviarCorreo(eq("juan@example.com"), anyString(), anyString());
    }

    @Test
    void solicitarRecuperacionPassword_UsuarioNoEncontrado_ThrowsException() {
        String email = "noexiste@example.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.solicitarRecuperacionPassword(email));

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    void solicitarRecuperacionPassword_SolicitudExitosa() {
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        assertDoesNotThrow(() -> authService.solicitarRecuperacionPassword("juan@example.com"));

        verify(usuarioRepository).save(argThat(u -> 
            u.getTokenRecuperacion() != null && 
            u.getExpiracionCodigo() != null
        ));
        verify(emailService).enviarCorreo(eq("juan@example.com"), anyString(), anyString());
    }

    @Test
    void restaurarPassword_TokenInvalido_ThrowsException() {
        String token = "invalid-token";
        String nuevaContrasenia = "newpassword123";
        
        when(usuarioRepository.findByTokenRecuperacion(token)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.restaurarPassword(token, nuevaContrasenia));

        assertTrue(exception.getMessage().contains("Token inválido"));
    }

    @Test
    void restaurarPassword_TokenExpirado_ThrowsException() {
        String token = "valid-token";
        String nuevaContrasenia = "newpassword123";
        
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionCodigo(LocalDateTime.now().minusMinutes(1));
        
        when(usuarioRepository.findByTokenRecuperacion(token)).thenReturn(Optional.of(usuario));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.restaurarPassword(token, nuevaContrasenia));

        assertTrue(exception.getMessage().contains("expirado"));
    }

    @Test
    void restaurarPassword_MismaContrasenia_ThrowsException() {
        String token = "valid-token";
        String nuevaContrasenia = "samepassword";
        
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(5));
        
        when(usuarioRepository.findByTokenRecuperacion(token)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(nuevaContrasenia, usuario.getContrasenia())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.restaurarPassword(token, nuevaContrasenia));

        assertTrue(exception.getMessage().contains("no puede ser igual a la anterior"));
    }

    @Test
    void restaurarPassword_RestauracionExitosa() {
        String token = "valid-token";
        String nuevaContrasenia = "newpassword123";
        
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(5));
        
        when(usuarioRepository.findByTokenRecuperacion(token)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(nuevaContrasenia, usuario.getContrasenia())).thenReturn(false);
        when(passwordEncoder.encode(nuevaContrasenia)).thenReturn("encodedNewPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        assertDoesNotThrow(() -> authService.restaurarPassword(token, nuevaContrasenia));

        verify(usuarioRepository).save(argThat(u -> 
            u.getContrasenia().equals("encodedNewPassword") && 
            u.getTokenRecuperacion() == null
        ));
    }
}
