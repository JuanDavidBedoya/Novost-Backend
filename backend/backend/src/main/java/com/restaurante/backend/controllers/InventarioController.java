package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.*;
import com.restaurante.backend.services.*;
import com.restaurante.backend.services.AuditService;
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
    private final TipoProductoService tipoProductoService;
    private final ProductoIndividualService productoIndividualService;
    private final AuditService auditService;

    // === TIPOS DE PRODUCTO ===
    
    @GetMapping("/tipos")
    public ResponseEntity<List<TipoProductoDTO>> obtenerTiposProducto() {
        return ResponseEntity.ok(tipoProductoService.obtenerTodos());
    }

    @PostMapping("/tipos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoProductoDTO> crearTipoProducto(@Valid @RequestBody TipoProductoDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoProductoService.crearTipo(request));
    }

    @PutMapping("/tipos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoProductoDTO> actualizarTipoProducto(
            @PathVariable Long id,
            @Valid @RequestBody TipoProductoDTO request) {
        return ResponseEntity.ok(tipoProductoService.actualizarTipo(id, request));
    }

    @DeleteMapping("/tipos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarTipoProducto(@PathVariable Long id) {
        tipoProductoService.eliminarTipo(id);
        return ResponseEntity.noContent().build();
    }

    // === PRODUCTOS POR TIPO ===
    
    @GetMapping("/tipos/{idTipo}/productos")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerProductosPorTipo(@PathVariable Long idTipo) {
        return ResponseEntity.ok(inventarioService.obtenerPorTipo(idTipo));
    }

    // === PRODUCTOS INDIVIDUALES ===
    
    @PostMapping("/entradas")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<ProductoIndividualDTO> registrarEntradaCompra(
            @Valid @RequestBody EntradaCompraRequestDTO request) {
        
        ProductoIndividualDTO nuevo = productoIndividualService.registrarEntradaCompra(request);
        
        auditService.logCreacion(AuditService.ENTIDAD_INVENTARIO, nuevo.getIdProducto(), 
            "Entrada de compra: " + request.getIdAlimento());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping("/productos/{idAlimento}/individuales")
    public ResponseEntity<List<ProductoIndividualDTO>> obtenerProductosIndividuales(
            @PathVariable Long idAlimento) {
        return ResponseEntity.ok(productoIndividualService.obtenerProductosPorAlimento(idAlimento));
    }

    @GetMapping("/alertas/vencimiento")
    public ResponseEntity<List<ProductoIndividualDTO>> obtenerAlertasVencimiento(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(productoIndividualService.obtenerProductosProximosAVencer(dias));
    }

    @GetMapping("/alertas/vencidos")
    public ResponseEntity<List<ProductoIndividualDTO>> obtenerProductosVencidos() {
        return ResponseEntity.ok(productoIndividualService.obtenerProductosVencidos());
    }

    // === ENDPOINTS EXISTENTES ===
    
    // Obtener todos los productos del inventario
    @GetMapping
    public ResponseEntity<List<InventarioResponseDTO>> obtenerTodos() {
        List<InventarioResponseDTO> inventarios = inventarioService.obtenerTodos();
        return ResponseEntity.ok(inventarios);
    }

    // Obtener producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<InventarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        InventarioResponseDTO inventario = inventarioService.obtenerPorId(id);
        return ResponseEntity.ok(inventario);
    }

    // Crear nuevo producto en inventario
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> crearProducto(@Valid @RequestBody InventarioRequestDTO request) {
        InventarioResponseDTO nuevo = inventarioService.crearProducto(request);
        
        // Log de creación
        auditService.logCreacion(AuditService.ENTIDAD_INVENTARIO, nuevo.getIdAlimento(), 
            "Creación de producto: " + request.getNombreAlimento());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // Actualizar producto
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody InventarioRequestDTO request) {
        InventarioResponseDTO actualizado = inventarioService.actualizarProducto(id, request);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_INVENTARIO, id, 
            "Actualización de producto: " + request.getNombreAlimento(), null, 
            "nombreAlimento: " + request.getNombreAlimento() + ", stockActual: " + request.getStockActual());
        
        return ResponseEntity.ok(actualizado);
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        inventarioService.eliminarProducto(id);
        
        // Log de eliminación
        auditService.logEliminacion(AuditService.ENTIDAD_INVENTARIO, id, 
            "Eliminación de producto del inventario ID: " + id);
        
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
        InventarioResponseDTO actualizado = inventarioService.agregarStock(id, cantidad);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_INVENTARIO, id, 
            "Agregar stock al producto", null, "cantidad agregada: " + cantidad);
        
        return ResponseEntity.ok(actualizado);
    }

    // Quitar stock (consumo)
    @PostMapping("/{id}/quitar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<InventarioResponseDTO> quitarStock(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        Double cantidad = request.get("cantidad");
        InventarioResponseDTO actualizado = inventarioService.quitarStock(id, cantidad);
        
        // Log de actualización
        auditService.logActualizacion(AuditService.ENTIDAD_INVENTARIO, id, 
            "Quitar stock del producto", null, "cantidad quitada: " + cantidad);
        
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/productos/individuales/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<Void> eliminarProductoIndividual(@PathVariable Long id) {
        productoIndividualService.eliminarProductoIndividual(id);
        return ResponseEntity.noContent().build();
    }

    // Reiniciar consumo del día (endpoint para cron job)
    @PostMapping("/reiniciar-consumo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reiniciarConsumoDiario() {
        inventarioService.reiniciarConsumoDiario();
        
        // Log de acción especial
        auditService.registrar(AuditService.ACCION_ACTUALIZAR, AuditService.ENTIDAD_INVENTARIO, null, 
            "Reinicio de consumo diario de inventario");
        
        return ResponseEntity.ok().build();
    }
}