package com.restaurante.backend.services;

import com.restaurante.backend.dtos.InventarioDashboardDTO;
import com.restaurante.backend.dtos.InventarioRequestDTO;
import com.restaurante.backend.dtos.InventarioResponseDTO;
import com.restaurante.backend.entities.Inventario;
import com.restaurante.backend.entities.Inventario.TipoMedida;
import com.restaurante.backend.entities.TipoProducto;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.exceptions.ValidationException;
import com.restaurante.backend.mappers.InventarioMapper;
import com.restaurante.backend.repositories.InventarioRepository;
import com.restaurante.backend.repositories.TipoProductoRepository;
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
    private final StockAlertaMetricaService stockAlertaMetricaService;
    private final IntegridadStockMetricaService integridadStockMetricaService;
    private final ModularidadInventarioMetricaService modularidadMetricaService;
    private final AdaptabilidadInventarioMetricaService adaptabilidadMetricaService;
    private final TipoProductoRepository tipoProductoRepository;
    private final AuditService auditService;

    // Crear nuevo producto en inventario
    @Transactional
    public InventarioResponseDTO crearProducto(InventarioRequestDTO request) {
        Inventario inventario = inventarioMapper.toEntity(request);
        
        if (request.getIdTipo() != null) {
            TipoProducto tipoProducto = tipoProductoRepository.findById(request.getIdTipo())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de producto no encontrado: " + request.getIdTipo()));
            inventario.setTipoProducto(tipoProducto);
        }
        
        Inventario saved = inventarioRepository.save(inventario);

        // MÉTRICA — registrar creación de nuevo producto con su unidad de medida
        adaptabilidadMetricaService.registrarProductoCreado(
                saved.getTipoMedida().name()
        );

        // Log de auditoría: crear nuevo producto en inventario
        try {
            auditService.registrar(
                AuditService.ACCION_CREAR,
                AuditService.ENTIDAD_INVENTARIO,
                saved.getIdAlimento(),
                "Creación de nuevo producto en inventario: " + saved.getNombreAlimento(),
                null,
                "stockActual: " + saved.getStockActual() + ", stockMinimo: " + saved.getStockMinimo() + 
                ", tipoMedida: " + saved.getTipoMedida() +
                (request.getIdTipo() != null ? ", idTipo: " + request.getIdTipo() : "")
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para creación de producto: " + e.getMessage());
        }

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

        // MÉTRICA — inicio de medición
        long tiempoInicio = System.currentTimeMillis();

        // Guardar valores anteriores para el log
        String nombreAnterior = inventario.getNombreAlimento();
        Double stockAnterior = inventario.getStockActual();
        Double stockMinimoAnterior = inventario.getStockMinimo();
        TipoMedida medidaAnterior = inventario.getTipoMedida();

        // Actualizar tipo de producto si se proporciona idTipo
        if (request.getIdTipo() != null) {
            TipoProducto tipoProducto = tipoProductoRepository.findById(request.getIdTipo())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de producto no encontrado: " + request.getIdTipo()));
            inventario.setTipoProducto(tipoProducto);
        } else {
            inventario.setTipoProducto(null);
        }

        inventarioMapper.updateEntity(inventario, request);
        Inventario saved = inventarioRepository.save(inventario);

        // MÉTRICA — fin de medición, registrar duración
        long duracionMs = System.currentTimeMillis() - tiempoInicio;
        modularidadMetricaService.registrarActualizacion(duracionMs);

        // Log de auditoría: actualizar producto
        try {
            String detalles = String.format("nombre: %s -> %s, stockActual: %.2f -> %.2f, stockMinimo: %.2f -> %.2f, tipoMedida: %s -> %s",
                nombreAnterior, saved.getNombreAlimento(),
                stockAnterior, saved.getStockActual(),
                stockMinimoAnterior, saved.getStockMinimo(),
                medidaAnterior, saved.getTipoMedida());
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                AuditService.ENTIDAD_INVENTARIO,
                id,
                "Actualización de producto: " + saved.getNombreAlimento(),
                null,
                detalles
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para actualización de producto: " + e.getMessage());
        }

        if (saved.getStockActual() < saved.getStockMinimo()) {
            notificarStockMinimo(saved);
        }

        return inventarioMapper.toResponseDTO(saved);
    }

    // Eliminar producto
    @Transactional
    public void eliminarProducto(Long id) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        
        String nombreProducto = inventario.getNombreAlimento();
        Double stockFinal = inventario.getStockActual();

        inventarioRepository.deleteById(id);

        // Log de auditoría: eliminar producto de inventario
        try {
            auditService.registrar(
                AuditService.ACCION_ELIMINAR,
                AuditService.ENTIDAD_INVENTARIO,
                id,
                "Eliminación de producto del inventario",
                null,
                "nombreAlimento: " + nombreProducto + ", stockFinal: " + stockFinal
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para eliminación de producto: " + e.getMessage());
        }
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

    // Obtener productos por tipo
    public List<InventarioResponseDTO> obtenerPorTipo(Long idTipo) {
        return inventarioRepository.findByTipoProducto(idTipo).stream()
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

        Double stockAnterior = inventario.getStockActual();
        inventario.setStockActual(inventario.getStockActual() + cantidad);
        inventario.setFechaActualizacion(LocalDate.now());

        Inventario saved = inventarioRepository.save(inventario);

        // Log de auditoría: agregar stock
        try {
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                AuditService.ENTIDAD_INVENTARIO,
                id,
                "Agregar stock al producto: " + inventario.getNombreAlimento(),
                "stockActual: " + stockAnterior,
                "cantidadAgregada: " + cantidad + ", nuevoStock: " + inventario.getStockActual()
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para agregar stock: " + e.getMessage());
        }

        return inventarioMapper.toResponseDTO(saved);
    }

    // Quitar stock (consumo)
    @Transactional
    public InventarioResponseDTO quitarStock(Long id, Double cantidad) {
        Inventario inventario = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        // MÉTRICA — se intentó un consumo
        integridadStockMetricaService.registrarIntento();

        if (cantidad <= 0) {
            // MÉTRICA — rechazado: cantidad inválida
            integridadStockMetricaService.registrarRechazado();
            throw new ValidationException("La cantidad debe ser mayor a 0");
        }

        if (inventario.getStockActual() < cantidad) {
            // MÉTRICA — rechazado: stock insuficiente (protege contra saldo negativo)
            integridadStockMetricaService.registrarRechazado();
            throw new ValidationException("Stock insuficiente. Stock actual: " + inventario.getStockActual());
        }

        // Llegó hasta aquí = consumo válido
        integridadStockMetricaService.registrarAceptado();

        Double stockAnterior = inventario.getStockActual();
        inventario.setUltimoConsumo(cantidad);
        inventario.setConsumoHoy(inventario.getConsumoHoy() + cantidad);
        inventario.setStockActual(inventario.getStockActual() - cantidad);
        inventario.setFechaActualizacion(LocalDate.now());

        Inventario saved = inventarioRepository.save(inventario);

        // Log de auditoría: quitar stock (consumo)
        try {
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                AuditService.ENTIDAD_INVENTARIO,
                id,
                "Quitar stock (consumo) del producto: " + inventario.getNombreAlimento(),
                "stockAnterior: " + stockAnterior,
                "cantidadQuitada: " + cantidad + ", nuevoStock: " + inventario.getStockActual()
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para quitar stock: " + e.getMessage());
        }

        if (saved.getStockActual() < saved.getStockMinimo()) {
            notificarStockMinimo(saved);
        }

        return inventarioMapper.toResponseDTO(saved);
    }

    // Reiniciar consumo del día (para el cron job)
    @Transactional
    public void reiniciarConsumoDiario() {
        List<Inventario> inventarios = inventarioRepository.findAll();
        int count = 0;
        for (Inventario inv : inventarios) {
            inv.setConsumoHoy(0.0);
            inv.setFechaActualizacion(LocalDate.now());
            inventarioRepository.save(inv);
            count++;
        }

        // Log de auditoría: reinicio de consumo diario
        try {
            auditService.registrar(
                AuditService.ACCION_ACTUALIZAR,
                AuditService.ENTIDAD_INVENTARIO,
                null,
                "Reinicio de consumo diario de inventario",
                null,
                "Total de productos actualizados: " + count
            );
        } catch (Exception e) {
            System.err.println("Error al registrar log de auditoría para reinicio de consumo: " + e.getMessage());
        }
    }

    // Notificar stock mínimo
    private void notificarStockMinimo(Inventario inventario) {
    // MÉTRICA — registrar que se intentó enviar una alerta
        stockAlertaMetricaService.registrarIntento();
        try {
            emailService.enviarAlertaStockMinimo(
                "alertas@novost.com",
                inventario.getNombreAlimento(),
                inventario.getStockActual(),
                inventario.getStockMinimo(),
                getUnidadDisplay(inventario)
            );

            // MÉTRICA — correo enviado correctamente
            stockAlertaMetricaService.registrarExito();

            // Log de auditoría: alerta de stock mínimo
            try {
                auditService.registrar(
                    AuditService.ACCION_ACTUALIZAR,
                    AuditService.ENTIDAD_INVENTARIO,
                    inventario.getIdAlimento(),
                    "Alerta de stock mínimo enviada",
                    null,
                    "producto: " + inventario.getNombreAlimento() + 
                    ", stockActual: " + inventario.getStockActual() + 
                    ", stockMinimo: " + inventario.getStockMinimo()
                );
            } catch (Exception e) {
                System.err.println("Error al registrar log de auditoría para alerta de stock: " + e.getMessage());
            }

        } catch (Exception e) {
            // MÉTRICA — el correo falló
            stockAlertaMetricaService.registrarFallo();
            System.err.println("Error al enviar notificación de stock mínimo: " + e.getMessage());
            
            // Log de auditoría: fallo al enviar alerta de stock mínimo
            try {
                auditService.registrar(
                    AuditService.ACCION_ERROR,
                    AuditService.ENTIDAD_INVENTARIO,
                    inventario.getIdAlimento(),
                    "Fallo al enviar alerta de stock mínimo",
                    null,
                    "producto: " + inventario.getNombreAlimento() + 
                    ", stockActual: " + inventario.getStockActual() + 
                    ", stockMinimo: " + inventario.getStockMinimo() + 
                    ", error: " + e.getMessage()
                );
            } catch (Exception ex) {
                System.err.println("Error al registrar log de auditoría para fallo de alerta: " + ex.getMessage());
            }
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
