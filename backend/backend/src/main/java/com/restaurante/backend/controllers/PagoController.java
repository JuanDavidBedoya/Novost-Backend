package com.restaurante.backend.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurante.backend.dtos.PagoRequestDTO;
import com.restaurante.backend.dtos.PagoResponseDTO;
import com.restaurante.backend.dtos.PedidoRequestDTO;
import com.restaurante.backend.services.AuditService;
import com.restaurante.backend.services.PagoService;
import com.restaurante.backend.services.PasarelaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final PasarelaService pasarelaService;
    private final AuditService auditService;

    @PostMapping("/crear-intento")
    public ResponseEntity<Map<String, String>> iniciarPago(@RequestParam(name = "idReserva") Long idReserva) {
        System.out.println("DEBUG: Recibida solicitud para reserva ID: " + idReserva);

        try {
            Map<String, String> datosStripe = pasarelaService.crearIntentoPago(idReserva);
            
            // Log de creación
            auditService.logCreacion(AuditService.ENTIDAD_PAGO, idReserva, 
                "Creación de intento de pago para reserva: " + idReserva);
            
            return ResponseEntity.ok(datosStripe);
        } catch (Exception e) {
            System.err.println("DEBUG: Error creando intento: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/confirmar") 
    public ResponseEntity<PagoResponseDTO> confirmarPago(@RequestBody PagoRequestDTO pagoRequest) {
        PagoResponseDTO respuesta = pagoService.procesarConfirmacionPago(pagoRequest);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_PAGO, pagoRequest.getIdReserva(), 
            "Confirmación de pago", null, "estado: PAGADO");
        
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/pedido/crear-intento-previo")
    public ResponseEntity<Map<String, String>> iniciarPagoPedidoPrevio(
            @RequestBody PedidoRequestDTO pedidoRequest) {
        try {
            Map<String, String> datosStripe =
                    pasarelaService.crearIntentoPagoPedidoPrevio(pedidoRequest);
            
            // Log de creación
            auditService.logCreacion(AuditService.ENTIDAD_PAGO, null, 
                "Creación de intento de pago previo para pedido");
            
            return ResponseEntity.ok(datosStripe);
        } catch (Exception e) {
            System.err.println("Error creando intento previo de pedido: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/pedido/crear-intento-existente")
    public ResponseEntity<Map<String, String>> iniciarPagoPedidoExistente(
            @RequestBody Map<String, Long> body) {
        try {
            Long idPedido = body.get("idPedido");
            Map<String, String> datosStripe =
                    pasarelaService.crearIntentoPagoPedidoExistente(idPedido);
            auditService.logCreacion(AuditService.ENTIDAD_PAGO, idPedido,
                    "Creación de intento de pago para pedido existente #" + idPedido);
            return ResponseEntity.ok(datosStripe);
        } catch (Exception e) {
            System.err.println("Error creando intento para pedido existente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
