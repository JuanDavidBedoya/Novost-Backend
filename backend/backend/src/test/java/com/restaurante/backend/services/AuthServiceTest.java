package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.*;
import com.restaurante.backend.mappers.*;
import com.restaurante.backend.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private UsuarioMapper usuarioMapper;

    @InjectMocks private AuthService authService;

    private Usuario usuario;
    private Rol rolUsuario;

    @BeforeEach
    void setUp() {
        rolUsuario = new Rol(1L, "USUARIO");
        usuario = new Usuario();
        usuario.setCedula("12345678");
        usuario.setNombre("Juan Pérez");
        usuario.setEmail("juan@test.com");
        usuario.setTelefono("3001234567");
        usuario.setContrasenia("encodedPassword");
        usuario.setRol(rolUsuario);
    }

    @Test
    void iniciarLogin_deberiaLanzarExcepcion_cuandoEmailNoExiste() {
        var request = new LoginRequestDTO("noexiste@test.com", "password123");
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        var ex = assertThrows(RuntimeException.class, () -> authService.iniciarLogin(request));
        assertNotNull(ex.getMessage());
    }

    @Test
    void iniciarLogin_deberiaLanzarExcepcion_cuandoPasswordIncorrecta() {
        var request = new LoginRequestDTO("juan@test.com", "wrongpass");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongpass", "encodedPassword")).thenReturn(false);

        var ex = assertThrows(RuntimeException.class, () -> authService.iniciarLogin(request));
        assertNotNull(ex.getMessage());
    }

    @Test
    void verificarCodigo_deberiaLanzarExcepcion_cuandoCodigoIncorrecto() {
        usuario.setCodigoVerificacion("123456");
        usuario.setExpiracionCodigo(LocalDateTime.now().plusMinutes(5));
        var request = new VerifyCodeDTO("juan@test.com", "wrongcode");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));

        var ex = assertThrows(RuntimeException.class, () -> authService.verificarCodigo(request));
        assertNotNull(ex.getMessage());
    }

    @Test
    void verificarCodigo_deberiaLanzarExcepcion_cuandoCodigoExpirado() {
        usuario.setCodigoVerificacion("123456");
        usuario.setExpiracionCodigo(LocalDateTime.now().minusMinutes(1));
        var request = new VerifyCodeDTO("juan@test.com", "123456");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));

        var ex = assertThrows(RuntimeException.class, () -> authService.verificarCodigo(request));
        assertNotNull(ex.getMessage());
    }

    @Test
    void restaurarPassword_deberiaLanzarExcepcion_cuandoTokenInvalido() {
        when(usuarioRepository.findByTokenRecuperacion("invalid")).thenReturn(Optional.empty());

        var ex = assertThrows(RuntimeException.class, 
            () -> authService.restaurarPassword("invalid", "newpass"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void solicitarRecuperacionPassword_deberiaEnviarEmail_cuandoUsuarioExiste() {
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));

        assertDoesNotThrow(() -> authService.solicitarRecuperacionPassword("juan@test.com"));
        verify(usuarioRepository).save(usuario);
    }
}