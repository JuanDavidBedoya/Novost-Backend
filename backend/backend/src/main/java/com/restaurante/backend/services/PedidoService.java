package com.restaurante.backend.services;

import com.restaurante.backend.dtos.PedidoDetalleRequestDTO;
import com.restaurante.backend.dtos.PedidoRequestDTO;
import com.restaurante.backend.dtos.PedidoResponseDTO;
import com.restaurante.backend.entities.*;
import com.restaurante.backend.exceptions.ResourceNotFoundException;
import com.restaurante.backend.exceptions.ValidationException;
import com.restaurante.backend.mappers.PedidoMapper;
import com.restaurante.backend.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final PedidoDetalleRepository pedidoDetalleRepo;
    private final MesaRepository mesaRepo;
    private final ReservaRepository reservaRepo;
    private final PlatoRepository platoRepo;
    private final PlatoDetalleRepository platoDetalleRepo;
    private final InventarioRepository inventarioRepo;
    private final EstadoPedidoRepository estadoPedidoRepo;
    private final PedidoMapper pedidoMapper;
    private final PlatoService platoService;
    private final PagoPedidoRepository pagoPedidoRepo;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepo;
    private final PedidoMetricaService pedidoMetricaService;

    private static final Double IMPUESTO_IVA = 0.19;

    @Transactional
    public PedidoResponseDTO crearPedido(PedidoRequestDTO dto) {

    // ✅ Registra el intento antes de cualquier lógica
    pedidoMetricaService.registrarIntento();

    // ✅ Envuelve toda la lógica en el timer para medir duración
    return pedidoMetricaService.getTiempoTimer().record(() -> {
        try {
            LocalDate fechaActual = LocalDate.now();
            LocalTime horaActual  = LocalTime.now();

            Mesa mesa = mesaRepo.findById(dto.getIdMesa())
                    .orElseThrow(() -> new ResourceNotFoundException("Mesa", dto.getIdMesa().toString()));

            List<Reserva> reservasActivas = reservaRepo.findReservaActivaPorMesaYHora(
            mesa.getIdMesa(), fechaActual, horaActual);

            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuarioLogueado = usuarioRepo.findByEmail(emailLogueado)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", emailLogueado));

            Pedido pedido = new Pedido();
            pedido.setMesa(mesa);
            pedido.setReserva(reservasActivas.isEmpty() ? null : reservasActivas.get(0));
            pedido.setUsuario(usuarioLogueado);
            pedido.setFechaPedido(fechaActual);
            pedido.setHoraPedido(horaActual);
            pedido.setObservaciones(dto.getObservaciones());

            EstadoPedido estadoInicial = estadoPedidoRepo.findByNombre("RECIBIDO")
                    .orElseThrow(() -> new ResourceNotFoundException("Estado de pedido RECIBIDO no configurado"));
            pedido.setEstadoPedido(estadoInicial);

            double subtotalAcumulado = 0.0;
            List<PedidoDetalle> detallesAGuardar = new ArrayList<>();

            for (PedidoDetalleRequestDTO detalleDto : dto.getDetalles()) {
                Plato plato = platoRepo.findById(detalleDto.getIdPlato())
                        .orElseThrow(() -> new ResourceNotFoundException("Plato", detalleDto.getIdPlato().toString()));

                if (Boolean.FALSE.equals(plato.getEstado())) {
                    throw new ValidationException("plato",
                            "El plato '" + plato.getNombrePlato() + "' no está disponible actualmente.");
                }

                List<PlatoDetalle> receta = platoDetalleRepo.findByPlato(plato);
                for (PlatoDetalle ingrediente : receta) {
                    Inventario itemInventario = ingrediente.getInventario();
                    double cantidadRequerida = ingrediente.getCantidadNecesaria() * detalleDto.getCantidad();

                    if (itemInventario.getStockActual() < cantidadRequerida) {
                        throw new ValidationException("inventario",
                                "Stock insuficiente para preparar '" + plato.getNombrePlato() +
                                "'. Faltan ingredientes (" + itemInventario.getNombreAlimento() + ").");
                    }

                    itemInventario.setUltimoConsumo(cantidadRequerida);
                    itemInventario.setConsumoHoy(itemInventario.getConsumoHoy() + cantidadRequerida);
                    itemInventario.setStockActual(itemInventario.getStockActual() - cantidadRequerida);
                    itemInventario.setFechaActualizacion(LocalDate.now());
                    inventarioRepo.save(itemInventario);
                }

                double subtotalLinea = plato.getPrecioPlato() * detalleDto.getCantidad();
                subtotalAcumulado += subtotalLinea;

                PedidoDetalle pd = new PedidoDetalle();
                pd.setPedido(pedido);
                pd.setPlato(plato);
                pd.setCantidad(detalleDto.getCantidad());
                pd.setPrecioUnitario(plato.getPrecioPlato());
                pd.setSubtotal(subtotalLinea);
                detallesAGuardar.add(pd);
            }

            pedido.setSubtotal(subtotalAcumulado);
            pedido.setTotal(subtotalAcumulado + (subtotalAcumulado * IMPUESTO_IVA));

            Pedido pedidoGuardado = pedidoRepo.save(pedido);
            pedidoDetalleRepo.saveAll(detallesAGuardar);

            platoService.recalcularDisponibilidadPorIngredientesAfectados(
                    dto.getDetalles().stream()
                            .map(d -> platoRepo.findById(d.getIdPlato()).orElseThrow())
                            .collect(Collectors.toList())
            );

            PagoPedido pagoPedido = new PagoPedido();
            pagoPedido.setPedido(pedidoGuardado);
            pagoPedido.setMetodoPago(dto.getMetodoPago());
            pagoPedido.setMonto(pedidoGuardado.getTotal());
            pagoPedido.setFechaPago(LocalDateTime.now());
            pagoPedido.setIdPasarela(null);
            pagoPedido.setEstadoPago("PENDIENTE");
            pagoPedidoRepo.save(pagoPedido);

            // ✅ Solo se registra éxito si todo el bloque anterior completó sin errores
            pedidoMetricaService.registrarExito();

            return pedidoMapper.toResponseDTO(pedidoGuardado);

        } catch (Exception e) {
            // ✅ Registra el fallo y relanza la excepción para que Spring la maneje normalmente
            pedidoMetricaService.registrarFallo();
            throw e;
        }
    });
}

    // ── Llamado por el webhook de Stripe (pago en línea confirmado) ───────────
    @Transactional
    public void confirmarPagoPedido(Long idPedido, String idPasarela) {
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", idPedido.toString()));

        PagoPedido pagoPedido = pagoPedidoRepo.findByPedido(pedido)
                .orElseThrow(() -> new ResourceNotFoundException("PagoPedido", idPedido.toString()));

        pagoPedido.setEstadoPago("PAGADO");
        pagoPedido.setIdPasarela(idPasarela);
        pagoPedido.setFechaPago(LocalDateTime.now());
        pagoPedidoRepo.save(pagoPedido);

        EstadoPedido estadoPagado = estadoPedidoRepo.findByNombre("PAGADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pedido PAGADO no configurado"));
        pedido.setEstadoPedido(estadoPagado);
        pedidoRepo.save(pedido);

        enviarFacturaDesidePedido(pagoPedido);
    }

    // ── Llamado por el trabajador desde GestionPedidos ────────────────────────
    @Transactional
    public PedidoResponseDTO avanzarEstadoPedido(Long idPedido) {

        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", idPedido.toString()));

        String estadoActual = pedido.getEstadoPedido().getNombre();

        String siguienteEstado = switch (estadoActual) {
            case "RECIBIDO" -> "PAGADO";
            case "PAGADO"   -> "ENTREGADO";
            default -> throw new ValidationException("estado",
                    "El pedido #" + idPedido + " ya está en estado '" + estadoActual
                    + "' y no puede avanzar más.");
        };

        EstadoPedido nuevoEstado = estadoPedidoRepo.findByNombre(siguienteEstado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estado de pedido " + siguienteEstado + " no configurado"));

        pedido.setEstadoPedido(nuevoEstado);
        Pedido actualizado = pedidoRepo.save(pedido);

        // Cuando el trabajador confirma el pago en caja:
        //   1. Marca el PagoPedido como PAGADO
        //   2. Envía la factura al correo del cliente
        // Los pedidos LINEA ya fueron confirmados por Stripe — no se tocan.
        if ("PAGADO".equals(siguienteEstado)) {
            pagoPedidoRepo.findByPedido(actualizado).ifPresent(pago -> {
                if ("CAJA".equalsIgnoreCase(pago.getMetodoPago())
                        && "PENDIENTE".equalsIgnoreCase(pago.getEstadoPago())) {

                    pago.setEstadoPago("PAGADO");
                    pago.setFechaPago(LocalDateTime.now());
                    pagoPedidoRepo.save(pago);

                    // Factura al cliente — usuario tomado del pedido, no del JWT
                    // (quien llama aquí es el trabajador, no el cliente)
                    enviarFacturaDesidePedido(pago);
                }
            });
        }

        return pedidoMapper.toResponseDTO(actualizado);
    }

    // ── Llamado por el webhook cuando el pago en línea es confirmado ──────────
    @Transactional
        public void crearYConfirmarPedidoLinea(PedidoRequestDTO dto, String idPasarela) {
        // ✅ crearPedido ya registra intento/exito/fallo, no se duplica aquí
        PedidoResponseDTO pedidoDTO = crearPedido(dto);

        Pedido pedido = pedidoRepo.findById(pedidoDTO.getIdPedido())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido", pedidoDTO.getIdPedido().toString()));

        confirmarPagoPedido(pedido.getIdPedido(), idPasarela);
        }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<PedidoResponseDTO> obtenerMisPedidos(String fechaStr, String estado) {
        String emailLogueado = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Usuario usuarioLogueado = usuarioRepo.findByEmail(emailLogueado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", emailLogueado));

        LocalDate fecha = (fechaStr != null && !fechaStr.isBlank())
                ? LocalDate.parse(fechaStr) : null;
        String estadoFiltro = (estado != null && !estado.isBlank()) ? estado : null;

        return pedidoRepo.findByUsuarioWithFilters(usuarioLogueado, fecha, estadoFiltro)
                .stream()
                .map(pedidoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PedidoResponseDTO> obtenerTodosLosPedidos(String fechaStr, String estado) {
        LocalDate fecha = (fechaStr != null && !fechaStr.isBlank())
                ? LocalDate.parse(fechaStr) : null;
        String estadoFiltro = (estado != null && !estado.isBlank()) ? estado : null;

        return pedidoRepo.findAllWithFilters(fecha, estadoFiltro)
                .stream()
                .map(pedidoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }


    private void enviarFacturaDesidePedido(PagoPedido pagoPedido) {
        try {
            Pedido pedido = pagoPedido.getPedido();
            String email;
            String nombre;
            Reserva reservaAsociada = pedido.getReserva();

            if (pedido.getUsuario() != null) {
                // Caso normal: el pedido tiene su propio usuario registrado
                email  = pedido.getUsuario().getEmail();
                nombre = pedido.getUsuario().getNombre();
            } else if (pedido.getReserva() != null && pedido.getReserva().getUsuario() != null) {
                // Fallback: tomar el usuario de la reserva asociada
                email  = pedido.getReserva().getUsuario().getEmail();
                nombre = pedido.getReserva().getUsuario().getNombre();
            } else {
                System.err.println("No se encontró usuario para enviar factura del pedido #"
                        + pedido.getIdPedido());
                return;
            }

            emailService.enviarFacturaPedido(pagoPedido, email, nombre, reservaAsociada);

        } catch (Exception e) {
            System.err.println("No se pudo enviar la factura del pedido: " + e.getMessage());
        }
    }
}