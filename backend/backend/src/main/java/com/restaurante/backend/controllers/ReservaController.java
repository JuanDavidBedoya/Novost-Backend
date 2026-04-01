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
import com.restaurante.backend.services.DisponibilidadMetricaService;

import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.ReservaConfirmarPagoRequestDTO;
import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.services.AuditService;
import com.restaurante.backend.services.ReservaService;
import com.restaurante.backend.services.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    private final AuditService auditService;
    private final DisponibilidadMetricaService disponibilidadMetricaService;

    @GetMapping("/buscar")
    public ResponseEntity<List<ReservaResponseDTO>> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer personas) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = authentication.getName();
        
        String cedulaUsuarioLogeado = usuarioService.obtenerCedulaPorEmail(emailUsuario);
        
        List<ReservaResponseDTO> reservas = reservaService.buscarReservasPorUsuarioConFiltros(cedulaUsuarioLogeado, fecha, hora, personas);
        return ResponseEntity.ok(reservas);
    }

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> reservar(@RequestBody ReservaRequestDTO reservaRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuario = authentication.getName();
        
        String cedulaUsuarioLogeado = usuarioService.obtenerCedulaPorEmail(emailUsuario);
        
        if (!reservaRequest.getCedulaUsuario().equals(cedulaUsuarioLogeado)) {
            auditService.registrarError(AuditService.ACCION_CREAR, AuditService.ENTIDAD_RESERVA, null, 
                "Usuario intentó crear reserva para otra persona", "No puedes crear una reserva para otra persona");
            throw new RuntimeException("No puedes crear una reserva para otra persona");
        }
        
        ReservaResponseDTO nuevaReserva = reservaService.crearReserva(reservaRequest);
        
        // Log de creación
        auditService.logCreacion(AuditService.ENTIDAD_RESERVA, nuevaReserva.getIdReserva(), 
            "Usuario creó nueva reserva para fecha: " + reservaRequest.getFecha() + " a las " + reservaRequest.getHoraInicio());
        
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
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_RESERVA, id, 
            "Reserva pagada", null, "estado: PAGADO");
        
        return ResponseEntity.ok(pagoRealizado);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelarReserva(@PathVariable Long id) {
        ReservaResponseDTO response = reservaService.cancelarReserva(id);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_RESERVA, id, 
            "Reserva cancelada", null, "estado: CANCELADA");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ReservaResponseDTO> finalizarReserva(@PathVariable Long id) {
        ReservaResponseDTO response = reservaService.finalizarReserva(id);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_RESERVA, id, 
            "Reserva finalizada", null, "estado: FINALIZADA");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/todas")
    public ResponseEntity<List<ReservaResponseDTO>> buscarTodasLasReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer personas) {
        
        List<ReservaResponseDTO> reservas = reservaService.buscarReservas(fecha, hora, personas);
        return ResponseEntity.ok(reservas);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Integer> obtenerMesasDisponibles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
            @RequestParam(required = false) Integer personas) {

        // MÉTRICA — registrar que llegó una consulta
        disponibilidadMetricaService.registrarConsulta();

        try {
            int disponibles = reservaService.contarMesasDisponibles(fecha, hora, personas);
            return ResponseEntity.ok(disponibles);
        } catch (Exception e) {
            // MÉTRICA — registrar el error 500 y recalcular MTBF
            disponibilidadMetricaService.registrarError500();
            throw e; // relanzar para que GlobalExceptionHandler lo maneje
        }
    }
}
