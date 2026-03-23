package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.DashboardClientesDTO;
import com.restaurante.backend.dtos.DashboardFinancieroDTO;
import com.restaurante.backend.dtos.DashboardPlatosDTO;
import com.restaurante.backend.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/finanzas")
    public ResponseEntity<DashboardFinancieroDTO> obtenerFinanzas() {
        return ResponseEntity.ok(dashboardService.obtenerDatosFinancieros());
    }


    @GetMapping("/platos")
    public ResponseEntity<DashboardPlatosDTO> obtenerPlatos() {
        return ResponseEntity.ok(dashboardService.obtenerDatosPlatos());
    }

    @GetMapping("/clientes")
    public ResponseEntity<DashboardClientesDTO> obtenerClientes() {
        return ResponseEntity.ok(dashboardService.obtenerDatosClientes());
    }
}