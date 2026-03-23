package com.restaurante.backend.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.ReservaConfirmarPagoRequestDTO;
import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.services.ReservaService;
import com.restaurante.backend.services.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioService usuarioService;

    @GetMapping("/buscar")
    public ResponseEntity<List<ReservaResponseDTO>> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer personas) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = authentication.getName();
        
        String cedulaUsuarioLogeado = usuarioService.obtenerCedulaPorEmail(emailUsuario);
        
        return ResponseEntity.ok(reservaService.buscarReservasPorUsuarioConFiltros(cedulaUsuarioLogeado, fecha, hora, personas));
    }

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> reservar(@RequestBody ReservaRequestDTO reservaRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = authentication.getName();
        
        String cedulaUsuarioLogeado = usuarioService.obtenerCedulaPorEmail(emailUsuario);
        
        if (!reservaRequest.getCedulaUsuario().equals(cedulaUsuarioLogeado)) {
            throw new RuntimeException("No puedes crear una reserva para otra persona");
        }
        
        ReservaResponseDTO nuevaReserva = reservaService.crearReserva(reservaRequest);
        return ResponseEntity.ok(nuevaReserva);
    }

    @PostMapping("/{id}/confirmar-pago")
    public ResponseEntity<PagoResponseDTO> confirmar(
            @PathVariable Long id, 
            @RequestBody ReservaConfirmarPagoRequestDTO pagoRequest) {
        
        PagoResponseDTO pagoRealizado = reservaService.procesarPagoReserva(
            id, 
            pagoRequest.getIdPasarela(), 
            pagoRequest.getMonto()
        );
        
        return ResponseEntity.ok(pagoRealizado);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelarReserva(@PathVariable Long id) {
        ReservaResponseDTO response = reservaService.cancelarReserva(id);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ReservaResponseDTO> finalizarReserva(@PathVariable Long id) {
        ReservaResponseDTO response = reservaService.finalizarReserva(id);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/todas")
    public ResponseEntity<List<ReservaResponseDTO>> buscarTodasLasReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer personas) {
        
        return ResponseEntity.ok(reservaService.buscarReservas(fecha, hora, personas));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Integer> obtenerMesasDisponibles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer personas) {
        
        int disponibles = reservaService.contarMesasDisponibles(fecha, hora, personas);
        return ResponseEntity.ok(disponibles);
    }
}
