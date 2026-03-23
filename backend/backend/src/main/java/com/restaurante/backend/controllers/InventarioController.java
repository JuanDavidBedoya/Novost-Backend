package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.InventarioDashboardDTO;
import com.restaurante.backend.dtos.InventarioRequestDTO;
import com.restaurante.backend.dtos.InventarioResponseDTO;
import com.restaurante.backend.services.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    // Obtener todos los productos del inventario
    @GetMapping
    public ResponseEntity<List<InventarioResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(inventarioService.obtenerTodos());
    }

    // Obtener producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<InventarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.obtenerPorId(id));
    }

    // Crear nuevo producto en inventario
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> crearProducto(@Valid @RequestBody InventarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.crearProducto(request));
    }

    // Actualizar producto
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody InventarioRequestDTO request) {
        return ResponseEntity.ok(inventarioService.actualizarProducto(id, request));
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        inventarioService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    // Obtener dashboard de inventario
    @GetMapping("/dashboard")
    public ResponseEntity<InventarioDashboardDTO> obtenerDashboard() {
        return ResponseEntity.ok(inventarioService.obtenerDashboard());
    }

    // Obtener productos con stock mínimo
    @GetMapping("/stock-minimo")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerStockMinimo() {
        return ResponseEntity.ok(inventarioService.obtenerProductosStockMinimo());
    }

    // Obtener productos más utilizados
    @GetMapping("/mas-utilizados")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerMasUtilizados() {
        return ResponseEntity.ok(inventarioService.obtenerProductosMasUtilizados());
    }

    // Agregar stock (ingreso de productos)
    @PostMapping("/{id}/agregar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> agregarStock(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        Double cantidad = request.get("cantidad");
        return ResponseEntity.ok(inventarioService.agregarStock(id, cantidad));
    }

    // Quitar stock (consumo)
    @PostMapping("/{id}/quitar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> quitarStock(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        Double cantidad = request.get("cantidad");
        return ResponseEntity.ok(inventarioService.quitarStock(id, cantidad));
    }

    // Reiniciar consumo del día (endpoint para cron job)
    @PostMapping("/reiniciar-consumo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reiniciarConsumoDiario() {
        inventarioService.reiniciarConsumoDiario();
        return ResponseEntity.ok().build();
    }
}
