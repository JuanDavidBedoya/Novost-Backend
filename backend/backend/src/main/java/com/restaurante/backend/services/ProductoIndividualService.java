package com.restaurante.backend.services;

import com.restaurante.backend.dtos.EntradaCompraRequestDTO;
import com.restaurante.backend.dtos.ProductoIndividualDTO;
import com.restaurante.backend.entities.Inventario;
import com.restaurante.backend.entities.ProductoIndividual;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.mappers.ProductoIndividualMapper;
import com.restaurante.backend.repositories.InventarioRepository;
import com.restaurante.backend.repositories.ProductoIndividualRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoIndividualService {

    private final ProductoIndividualRepository productoIndividualRepository;
    private final InventarioRepository inventarioRepository;
    private final ProductoIndividualMapper mapper;
    private final AuditService auditService;

    @Transactional
    public ProductoIndividualDTO registrarEntradaCompra(EntradaCompraRequestDTO request) {
        Inventario inventario = inventarioRepository.findById(request.getIdAlimento())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + request.getIdAlimento()));

        ProductoIndividual producto = new ProductoIndividual();
        producto.setInventario(inventario);
        producto.setCantidad(request.getCantidad());
        producto.setFechaVencimiento(request.getFechaVencimiento());
        producto.setLote(request.getLote());
        producto.setPrecioCompra(request.getPrecioCompra());
        producto.setProveedor(request.getProveedor());
        producto.setEstado("DISPONIBLE");
        producto.setFechaIngreso(LocalDate.now());
        producto.setCedulaTrabajador(request.getCedulaTrabajador());

        ProductoIndividual saved = productoIndividualRepository.save(producto);

        inventario.setStockActual(inventario.getStockActual() + request.getCantidad());
        inventarioRepository.save(inventario);

        // Log de auditoría: entrada de compra
        try {
            auditService.registrar(
                AuditService.ACCION_CREAR,
                AuditService.ENTIDAD_INVENTARIO,
                saved.getIdProducto(),
                "Entrada de compra - Producto individual: " + inventario.getNombreAlimento(),
                null,
                "Cantidad: " + request.getCantidad() + ", Lote: " + request.getLote() + 
                ", Vencimiento: " + request.getFechaVencimiento() + ", Cedula Trabajador: " + request.getCedulaTrabajador()
            );
        } catch (Exception e) {
            // El logging no debe fallar la operación principal
            System.err.println("Error al registrar log de auditoría para entrada de compra: " + e.getMessage());
        }

        return mapper.toDTO(saved);
    }

    public List<ProductoIndividualDTO> obtenerProductosPorAlimento(Long idAlimento) {
        return productoIndividualRepository.findProductosDisponiblesByAlimento(idAlimento).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductoIndividualDTO> obtenerProductosPorTipo(Long idTipo) {
        return productoIndividualRepository.findByTipoProducto(idTipo).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductoIndividualDTO> obtenerProductosProximosAVencer(int dias) {
        LocalDate fechaLimite = LocalDate.now().plusDays(dias);
        return productoIndividualRepository.findProductosProximosAVencer(fechaLimite).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductoIndividualDTO> obtenerProductosVencidos() {
        return productoIndividualRepository.findProductosVencidos(LocalDate.now()).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marcarProductoComoAgotado(Long idProducto) {
        ProductoIndividual producto = productoIndividualRepository.findById(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto individual no encontrado: " + idProducto));
        producto.setEstado("AGOTADO");
        productoIndividualRepository.save(producto);

        Inventario inventario = producto.getInventario();
        Double nuevoStock = inventario.getStockActual() - producto.getCantidad();
        inventario.setStockActual(Math.max(0, nuevoStock));
        inventarioRepository.save(inventario);

        // Log de auditoría: producto marcado como agotado
        try {
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                AuditService.ENTIDAD_INVENTARIO,
                idProducto,
                "Producto individual marcado como AGOTADO: " + inventario.getNombreAlimento(),
                null,
                "Cantidad removida del stock: " + producto.getCantidad() + 
                ", Nuevo stock: " + inventario.getStockActual() +
                ", Cedula Trabajador: " + producto.getCedulaTrabajador()
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para producto agotado: " + e.getMessage());
        }
    }

    @Transactional
    public void verificarYMarcarVencidos() {
        List<ProductoIndividual> vencidos = productoIndividualRepository.findProductosVencidos(LocalDate.now());
        int count = 0;
        for (ProductoIndividual producto : vencidos) {
            producto.setEstado("VENCIDO");
            productoIndividualRepository.save(producto);
            count++;
        }

        // Log de auditoría: verificación de vencimiento
        try {
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                AuditService.ENTIDAD_INVENTARIO,
                null,
                "Verificación de vencimiento de productos individuales",
                null,
                "Total de productos marcados como VENCIDOS: " + count
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para verificación de vencimiento: " + e.getMessage());
        }
    }

    @Transactional
    public void eliminarProductoIndividual(Long idProducto) {
        ProductoIndividual producto = productoIndividualRepository.findByIdWithInventario(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto individual no encontrado: " + idProducto));
        
        Inventario inventario = producto.getInventario();
        Double nuevoStock = inventario.getStockActual() - producto.getCantidad();
        inventario.setStockActual(Math.max(0, nuevoStock));
        inventarioRepository.save(inventario);
        
        // Log de auditoría: eliminación de producto individual
        try {
            auditService.registrar(
                AuditService.ACCION_ELIMINAR,
                AuditService.ENTIDAD_INVENTARIO,
                idProducto,
                "Eliminación de producto individual: " + inventario.getNombreAlimento(),
                null,
                "Cantidad eliminada: " + producto.getCantidad() + 
                ", Lote: " + producto.getLote() + 
                ", Vencimiento: " + producto.getFechaVencimiento() +
                ", Cedula Trabajador: " + producto.getCedulaTrabajador()
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para eliminación de producto: " + e.getMessage());
        }
        
        productoIndividualRepository.deleteById(idProducto);
    }
}