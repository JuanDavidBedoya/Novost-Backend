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
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
        // Inyectamos una clave falsa para el @Value en la prueba
        ReflectionTestUtils.setField(authService, "recaptchaSecret", "dummy-secret-key");

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

        // Actualizamos el DTO para incluir el token del captcha
        registroRequest = new RegistroUsuarioDTO(
            "12345678",
            "Juan Perez",
            "juan@example.com",
            "3001234567",
            "password123",
            "token-valido-de-prueba" // <-- Nuevo campo del captcha
        );
    }

    // --- NUEVO: Helper para simular la respuesta de Google ---
    @SuppressWarnings({"unchecked"})
    private MockedConstruction<RestTemplate> mockearRespuestaGoogle(boolean isSuccess) {
        return mockConstruction(RestTemplate.class, (mock, context) -> {
            Map<String, Object> body = new HashMap<>();
            body.put("success", isSuccess);
            ResponseEntity<Object> responseObj = new ResponseEntity<>((Object) body, HttpStatus.OK);
            when(mock.postForEntity(any(), any(), any())).thenReturn(responseObj);
        });
    }

    // --- NUEVO: Test para cuando falla el Captcha ---
    @Test
    void registrar_CaptchaInvalido_ThrowsException() {
        // Simulamos que Google responde "success: false" (es un robot)
        try (MockedConstruction<RestTemplate> mocked = mockearRespuestaGoogle(false)) {
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.registrar(registroRequest));

            assertTrue(exception.getMessage().contains("Validación de captcha fallida"));
        }
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
        // Simulamos que Google dice "ok, es humano"
        try (MockedConstruction<RestTemplate> mocked = mockearRespuestaGoogle(true)) {
            when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.registrar(registroRequest));

            assertTrue(exception.getMessage().contains("Cédula duplicada"));
        }
    }

    @Test
    void registrar_EmailDuplicado_ThrowsException() {
        try (MockedConstruction<RestTemplate> mocked = mockearRespuestaGoogle(true)) {
            when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(false);
            when(usuarioRepository.findByEmail(registroRequest.email())).thenReturn(Optional.of(usuario));

            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.registrar(registroRequest));

            assertTrue(exception.getMessage().contains("Email duplicado"));
        }
    }

    @Test
    void registrar_RolNoEncontrado_ThrowsException() {
        try (MockedConstruction<RestTemplate> mocked = mockearRespuestaGoogle(true)) {
            when(usuarioRepository.existsById(registroRequest.cedula())).thenReturn(false);
            when(usuarioRepository.findByEmail(registroRequest.email())).thenReturn(Optional.empty());
            when(rolRepository.findById(1L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> authService.registrar(registroRequest));

            assertTrue(exception.getMessage().contains("Rol no encontrado"));
        }
    }

    @Test
    void registrar_RegistroExitoso_ReturnsUsuario() {
        try (MockedConstruction<RestTemplate> mocked = mockearRespuestaGoogle(true)) {
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
    }

    @Test
    void registrarTrabajador_RegistroExitoso_ReturnsUsuario() {
        // En tu código de AuthService, asumo que registrarTrabajador NO valida el captcha, 
        // por lo que no necesitamos envolverlo en el try de MockedConstruction.
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