package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.MenuItemDTO;
import com.restaurante.backend.services.PlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final PlatoService platoService;

    @GetMapping("/fuertes")
    public List<MenuItemDTO> getFuertes() { return platoService.obtenerPlatosPorCategoria("Fuertes"); }

    @GetMapping("/bebidas")
    public List<MenuItemDTO> getBebidas() { return platoService.obtenerPlatosPorCategoria("Bebidas"); }

    @GetMapping("/postres")
    public List<MenuItemDTO> getPostres() { return platoService.obtenerPlatosPorCategoria("Postres"); }

    @GetMapping("/entradas")
    public List<MenuItemDTO> getEntradas() { return platoService.obtenerPlatosPorCategoria("Entradas"); }

    // Endpoint para guardar URL de imagen de un plato
    @PostMapping("/{idPlato}/imagen")
    public ResponseEntity<Map<String, String>> guardarImagenUrl(
            @PathVariable Long idPlato,
            @RequestParam("imagenUrl") String imagenUrl) {
        try {
            String url = platoService.guardarImagenUrl(idPlato, imagenUrl);
            return ResponseEntity.ok(Map.of("imagenUrl", url));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}