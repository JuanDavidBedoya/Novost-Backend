package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.MenuItemDTO;
import com.restaurante.backend.services.PlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final PlatoService platoService; // 👈 Ahora usa el servicio

    @GetMapping("/fuertes")
    public List<MenuItemDTO> getFuertes() {
        return platoService.obtenerPlatosPorCategoria("Fuertes");
    }

    @GetMapping("/bebidas")
    public List<MenuItemDTO> getBebidas() {
        return platoService.obtenerPlatosPorCategoria("Bebidas");
    }

    @GetMapping("/postres")
    public List<MenuItemDTO> getPostres() {
        return platoService.obtenerPlatosPorCategoria("Postres");
    }

    @GetMapping("/entradas")
    public List<MenuItemDTO> getEntradas() {
        return platoService.obtenerPlatosPorCategoria("Entradas");
    }
}