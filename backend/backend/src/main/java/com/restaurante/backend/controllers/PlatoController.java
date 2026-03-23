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

    private final PlatoService platoService;

    @GetMapping("/fuertes")
    public List<MenuItemDTO> getFuertes() {
        List<MenuItemDTO> platos = platoService.obtenerPlatosPorCategoria("Fuertes");
        return platos;
    }

    @GetMapping("/bebidas")
    public List<MenuItemDTO> getBebidas() {
        List<MenuItemDTO> platos = platoService.obtenerPlatosPorCategoria("Bebidas");
        return platos;
    }

    @GetMapping("/postres")
    public List<MenuItemDTO> getPostres() {
        List<MenuItemDTO> platos = platoService.obtenerPlatosPorCategoria("Postres");
        return platos;
    }

    @GetMapping("/entradas")
    public List<MenuItemDTO> getEntradas() {
        List<MenuItemDTO> platos = platoService.obtenerPlatosPorCategoria("Entradas");
        return platos;
    }
}