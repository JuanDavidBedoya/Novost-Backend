package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.DashboardClientesDTO;
import com.restaurante.backend.dtos.DashboardFinancieroDTO;
import com.restaurante.backend.dtos.DashboardPlatosDTO;
import com.restaurante.backend.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/finanzas")
    public ResponseEntity<DashboardFinancieroDTO> obtenerFinanzas() {
        DashboardFinancieroDTO datos = dashboardService.obtenerDatosFinancieros();
        return ResponseEntity.ok(datos);
    }

    @GetMapping("/platos")
    public ResponseEntity<DashboardPlatosDTO> obtenerPlatos() {
        DashboardPlatosDTO datos = dashboardService.obtenerDatosPlatos();
        return ResponseEntity.ok(datos);
    }

    @GetMapping("/clientes")
    public ResponseEntity<DashboardClientesDTO> obtenerClientes() {
        DashboardClientesDTO datos = dashboardService.obtenerDatosClientes();
        return ResponseEntity.ok(datos);
    }
}