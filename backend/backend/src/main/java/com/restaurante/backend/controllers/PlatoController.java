package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.entities.Categoria;
import com.restaurante.backend.repositories.CategoriaRepository;
import com.restaurante.backend.services.AuditService;
import com.restaurante.backend.services.PlatoService;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final PlatoService platoService;
    private final CategoriaRepository categoriaRepository;
    private final AuditService auditService;

    // ── Endpoints existentes ────────────────────────────────────────
    @GetMapping("/fuertes")
    public List<MenuItemDTO> getFuertes()  { return platoService.obtenerPlatosPorCategoria("Fuertes"); }

    @GetMapping("/bebidas")
    public List<MenuItemDTO> getBebidas()  { return platoService.obtenerPlatosPorCategoria("Bebidas"); }

    @GetMapping("/postres")
    public List<MenuItemDTO> getPostres()  { return platoService.obtenerPlatosPorCategoria("Postres"); }

    @GetMapping("/entradas")
    public List<MenuItemDTO> getEntradas() { return platoService.obtenerPlatosPorCategoria("Entradas"); }

    @PostMapping("/{idPlato}/imagen")
    public ResponseEntity<Map<String, String>> guardarImagenUrl(
            @PathVariable Long idPlato,
            @RequestParam("imagenUrl") String imagenUrl) {
        try {
            String url = platoService.guardarImagenUrl(idPlato, imagenUrl);

            auditService.logActualizacion(AuditService.ENTIDAD_PLATO, idPlato,
                "Imagen actualizada para plato #" + idPlato, null, "imagenUrl: " + url);

            return ResponseEntity.ok(Map.of("imagenUrl", url));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /platos/categorias → lista todas las categorías
    @GetMapping("/categorias")
    public List<Categoria> getCategorias() {
        return categoriaRepository.findAll();
    }

    // GET /platos/ingredientes?q=query → busca ingredientes por nombre
    @GetMapping("/ingredientes")
    public List<InventarioItemDTO> buscarIngredientes(@RequestParam String q) {
        return platoService.buscarIngredientes(q);
    }

    // GET /platos/admin/todos → todos los platos para el panel admin
    @GetMapping("/admin/todos")
    public List<PlatoAdminDTO> getTodosAdmin() {
        return platoService.obtenerTodosLosPlatos();
    }

    // POST /platos → crear nuevo plato
    @PostMapping
    public ResponseEntity<Map<String, String>> crearPlato(@RequestBody CrearPlatoRequestDTO dto) {
        try {
            platoService.crearPlato(dto);

            auditService.logCreacion(AuditService.ENTIDAD_PLATO, null,
                "Creación de nuevo plato: " + dto.getNombrePlato());

            return ResponseEntity.ok(Map.of("mensaje", "Plato creado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /platos/{id}/habilitar → toggle habilitado/deshabilitado
    @PutMapping("/{idPlato}/habilitar")
    public ResponseEntity<Map<String, Object>> toggleHabilitar(@PathVariable Long idPlato) {
        try {
            boolean nuevoEstado = platoService.toggleHabilitadoAdmin(idPlato);

            auditService.logActualizacion(AuditService.ENTIDAD_PLATO, idPlato,
                "Toggle habilitado admin para plato #" + idPlato, null,
                "habilitadoAdmin: " + nuevoEstado);

            return ResponseEntity.ok(Map.of("habilitadoAdmin", nuevoEstado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{idPlato}/ultimo-cambio")
        public ResponseEntity<Map<String, Object>> getUltimoCambio(@PathVariable Long idPlato) {
            Instant ts = platoService.getUltimoToggleTimestamp(idPlato);
            if (ts == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(Map.of(
                "idPlato", idPlato,
                "timestampMs", ts.toEpochMilli()
            ));
        }

}