package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.PedidoRequestDTO;
import com.restaurante.backend.dtos.PedidoResponseDTO;
import com.restaurante.backend.services.AuditService;
import com.restaurante.backend.services.PedidoMetricaService;
import com.restaurante.backend.services.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final AuditService auditService;
    private final PedidoMetricaService pedidoMetricaService;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crearPedido(
            @Valid @RequestBody PedidoRequestDTO pedidoRequest) {
        PedidoResponseDTO nuevoPedido = pedidoService.crearPedido(pedidoRequest);
        
        // Log de creación
        auditService.logCreacion(AuditService.ENTIDAD_PEDIDO, nuevoPedido.getIdPedido(), 
            "Creación de nuevo pedido");
        
        return ResponseEntity.ok(nuevoPedido);
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> obtenerMisPedidos(
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String estado) {
        List<PedidoResponseDTO> pedidos = pedidoService.obtenerMisPedidos(fecha, estado);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMIN')")
    public ResponseEntity<List<PedidoResponseDTO>> obtenerTodosLosPedidos(
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String estado) {
        List<PedidoResponseDTO> pedidos = pedidoService.obtenerTodosLosPedidos(fecha, estado);
        
        // Log de consulta (admin/trabajador)
        auditService.logConsulta(AuditService.ENTIDAD_PEDIDO, null, 
            "Admin/Trabajador consultó todos los pedidos");
        
        return ResponseEntity.ok(pedidos);
    }
 
    @PatchMapping("/{id}/avanzar-estado")
    @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMIN')")
    public ResponseEntity<PedidoResponseDTO> avanzarEstado(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.avanzarEstadoPedido(id);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_PEDIDO, id, 
            "Avance de estado del pedido", null, "nuevo estado: " + pedido.getEstadoPedido());
        
        return ResponseEntity.ok(pedido);
    }

    @GetMapping("/metricas/rnf-07")
        public ResponseEntity<Map<String, Object>> resumenRnf07() {
            double intentos  = pedidoMetricaService.getIntentos();
            double exitosos  = pedidoMetricaService.getExitosos();
            double fallidos  = pedidoMetricaService.getFallidos();
            double tasa      = intentos > 0 ? (exitosos / intentos) * 100 : 0;
            boolean cumple   = tasa > 95;

            return ResponseEntity.ok(Map.of(
                    "intentosTotales",   intentos,
                    "pedidosExitosos",   exitosos,
                    "pedidosFallidos",   fallidos,
                    "tasaExitoPorc",     String.format("%.2f%%", tasa),
                    "criterio",          "(exitosos / intentos) × 100 > 95%",
                    "cumpleRNF",         cumple
            ));
        }
}