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
        
        // Obtener el usuario autenticado desde el token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = authentication.getName();
        
        // Obtener la cédula del usuario logeado
        String cedulaUsuarioLogeado = usuarioService.obtenerCedulaPorEmail(emailUsuario);
        
        // Retornar solo las reservas del usuario autenticado con los filtros aplicados
        return ResponseEntity.ok(reservaService.buscarReservasPorUsuarioConFiltros(cedulaUsuarioLogeado, fecha, hora, personas));
    }

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> reservar(@RequestBody ReservaRequestDTO reservaRequest) {
        // Obtener el usuario autenticado desde el token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = authentication.getName();
        
        // Obtener la cédula del usuario logeado
        String cedulaUsuarioLogeado = usuarioService.obtenerCedulaPorEmail(emailUsuario);
        
        // Validar que la cédula ingresada sea la misma del usuario logeado
        if (!reservaRequest.getCedulaUsuario().equals(cedulaUsuarioLogeado)) {
            throw new RuntimeException("No puedes crear una reserva para otra persona");
        }
        
        // El service ahora se encarga de convertir el DTO a entidad y viceversa
        ReservaResponseDTO nuevaReserva = reservaService.crearReserva(reservaRequest);
        return ResponseEntity.ok(nuevaReserva);
    }

    // Confirma el pago que viene de la pasarela externa
    @PostMapping("/{id}/confirmar-pago")
    public ResponseEntity<PagoResponseDTO> confirmar(
            @PathVariable Long id, 
            @RequestBody ReservaConfirmarPagoRequestDTO pagoRequest) {
        
        // El service procesa el pago, genera la factura por email y actualiza la reserva
        PagoResponseDTO pagoRealizado = reservaService.procesarPagoReserva(
            id, 
            pagoRequest.getIdPasarela(), 
            pagoRequest.getMonto()
        );
        
        return ResponseEntity.ok(pagoRealizado);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelarReserva(@PathVariable Long id) {
        // Llamamos al servicio para ejecutar la lógica de negocio
        ReservaResponseDTO response = reservaService.cancelarReserva(id);
        
        // Retornamos la reserva actualizada con su nuevo estado
        return ResponseEntity.ok(response);
    }
}
