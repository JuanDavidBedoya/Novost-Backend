package com.restaurante.backend.services;

import com.restaurante.backend.dtos.InventarioDashboardDTO;
import com.restaurante.backend.dtos.InventarioRequestDTO;
import com.restaurante.backend.dtos.InventarioResponseDTO;
import com.restaurante.backend.entities.Inventario;
import com.restaurante.backend.entities.Inventario.TipoMedida;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.exceptions.ValidationException;
import com.restaurante.backend.mappers.InventarioMapper;
import com.restaurante.backend.repositories.InventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final InventarioMapper inventarioMapper;
    private final EmailService emailService;

    // Crear nuevo producto en inventario
    @Transactional
    public InventarioResponseDTO crearProducto(InventarioRequestDTO request) {
        Inventario inventario = inventarioMapper.toEntity(request);
        Inventario saved = inventarioRepository.save(inventario);

        // Verificar si está por debajo del stock mínimo
        if (saved.getStockActual() < saved.getStockMinimo()) {
            notificarStockMinimo(saved);
        }

        return inventarioMapper.toResponseDTO(saved);
    }

    // Actualizar producto
    @Transactional
    public InventarioResponseDTO actualizarProducto(Long id, InventarioRequestDTO request) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        inventarioMapper.updateEntity(inventario, request);

        Inventario saved = inventarioRepository.save(inventario);

        // Verificar si está por debajo del stock mínimo
        if (saved.getStockActual() < saved.getStockMinimo()) {
            notificarStockMinimo(saved);
        }

        return inventarioMapper.toResponseDTO(saved);
    }

    // Eliminar producto
    @Transactional
    public void eliminarProducto(Long id) {
        if (!inventarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        inventarioRepository.deleteById(id);
    }

    // Obtener todos los productos
    public List<InventarioResponseDTO> obtenerTodos() {
        return inventarioRepository.findAll().stream()
                .map(inventarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Obtener producto por ID
    public InventarioResponseDTO obtenerPorId(Long id) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return inventarioMapper.toResponseDTO(inventario);
    }

    // Obtener productos con stock mínimo
    public List<InventarioResponseDTO> obtenerProductosStockMinimo() {
        return inventarioRepository.findProductosStockMinimo().stream()
                .map(inventarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Obtener productos más utilizados
    public List<InventarioResponseDTO> obtenerProductosMasUtilizados() {
        return inventarioRepository.findProductosMasUtilizados().stream()
                .map(inventarioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Dashboard de inventario
    public InventarioDashboardDTO obtenerDashboard() {
        List<Inventario> stockMinimo = inventarioRepository.findProductosStockMinimo();
        List<Inventario> masUtilizados = inventarioRepository.findProductosMasUtilizados();

        InventarioDashboardDTO dashboard = new InventarioDashboardDTO();
        dashboard.setProductosStockMinimo(stockMinimo.stream()
                .map(inventarioMapper::toResponseDTO)
                .collect(Collectors.toList()));
        dashboard.setProductosMasUtilizados(masUtilizados.stream()
                .map(inventarioMapper::toResponseDTO)
                .collect(Collectors.toList()));

        // Resumen general
        InventarioDashboardDTO.ResumenInventarioDTO resumen = new InventarioDashboardDTO.ResumenInventarioDTO();
        resumen.setTotalProductos(inventarioRepository.countTotalProductos());
        resumen.setProductosPorDebajoMinimo(inventarioRepository.countProductosStockMinimo());
        resumen.setStockTotalKilos(inventarioRepository.sumStockByTipoMedida(TipoMedida.KILO));
        resumen.setStockTotalLitros(inventarioRepository.sumStockByTipoMedida(TipoMedida.LITRO));
        resumen.setTotalUnidades(inventarioRepository.countUnidades());
        dashboard.setResumenGeneral(resumen);

        // Consumo del día
        dashboard.setConsumoTotalDia(inventarioRepository.sumConsumoHoy());

        return dashboard;
    }

    // Agregar stock (ingreso de productos)
    @Transactional
    public InventarioResponseDTO agregarStock(Long id, Double cantidad) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (cantidad <= 0) {
            throw new ValidationException("La cantidad debe ser mayor a 0");
        }

        inventario.setStockActual(inventario.getStockActual() + cantidad);
        inventario.setFechaActualizacion(LocalDate.now());

        Inventario saved = inventarioRepository.save(inventario);
        return inventarioMapper.toResponseDTO(saved);
    }

    // Quitar stock (consumo)
    @Transactional
    public InventarioResponseDTO quitarStock(Long id, Double cantidad) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (cantidad <= 0) {
            throw new ValidationException("La cantidad debe ser mayor a 0");
        }

        if (inventario.getStockActual() < cantidad) {
            throw new ValidationException("Stock insuficiente. Stock actual: " + inventario.getStockActual());
        }

        // Registrar consumo
        inventario.setUltimoConsumo(cantidad);
        inventario.setConsumoHoy(inventario.getConsumoHoy() + cantidad);
        inventario.setStockActual(inventario.getStockActual() - cantidad);
        inventario.setFechaActualizacion(LocalDate.now());

        Inventario saved = inventarioRepository.save(inventario);

        // Verificar si está por debajo del stock mínimo
        if (saved.getStockActual() < saved.getStockMinimo()) {
            notificarStockMinimo(saved);
        }

        return inventarioMapper.toResponseDTO(saved);
    }

    // Reiniciar consumo del día (para el cron job)
    @Transactional
    public void reiniciarConsumoDiario() {
        List<Inventario> inventarios = inventarioRepository.findAll();
        for (Inventario inv : inventarios) {
            inv.setConsumoHoy(0.0);
            inv.setFechaActualizacion(LocalDate.now());
            inventarioRepository.save(inv);
        }
    }

    // Notificar stock mínimo
    private void notificarStockMinimo(Inventario inventario) {
        try {
            String mensaje = String.format(
                "ALERTA DE INVENTARIO: El producto %s está por debajo del stock mínimo.\n" +
                "Stock actual: %s %s\n" +
                "Stock mínimo: %s %s\n" +
                "Por favor, realizar pedido de reposición.",
                inventario.getNombreAlimento(),
                inventario.getStockActual(),
                getUnidadDisplay(inventario),
                inventario.getStockMinimo(),
                getUnidadDisplay(inventario)
            );
            emailService.enviarCorreo("alertas@novost.com", "Alerta de Stock Mínimo", mensaje);
        } catch (Exception e) {
            // Log de error pero no fallar la operación
            System.err.println("Error al enviar notificación de stock mínimo: " + e.getMessage());
        }
    }

    private String getUnidadDisplay(Inventario inventario) {
        switch (inventario.getTipoMedida()) {
            case KILO:
                return "kg";
            case LITRO:
                return "L";
            case UNIDAD:
                return "und";
            default:
                return "";
        }
    }


}
