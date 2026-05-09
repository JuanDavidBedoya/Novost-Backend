package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import java.time.DayOfWeek;
import com.restaurante.backend.entities.PagoPedido;
import com.restaurante.backend.repositories.PagoPedidoRepository;
import com.restaurante.backend.repositories.PedidoDetalleRepository;
import com.restaurante.backend.repositories.PedidoRepository;
import com.restaurante.backend.repositories.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PagoPedidoRepository     pagoPedidoRepository;
    private final PedidoDetalleRepository  pedidoDetalleRepository;
    private final PedidoRepository          pedidoRepository;
    private final ReservaRepository         reservaRepository;

    // ── Dashboard Financiero ──────────────────────────────────────────────────

    public DashboardFinancieroDTO obtenerDatosFinancieros() {
        LocalDate hoy = LocalDate.now();
 
        // Rangos de tiempo
        LocalDateTime inicioDia    = hoy.atStartOfDay();
        LocalDateTime finDia       = hoy.atTime(LocalTime.MAX);
        LocalDateTime inicioSemana = hoy.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime finSemana    = hoy.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        LocalDateTime inicioMes    = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes       = hoy.atTime(LocalTime.MAX);
 
        // Solo pagos confirmados (estadoPago = "PAGADO") de la entidad pago_pedido
        List<PagoPedido> pagosHoy    = pagoPedidoRepository.findByFechaPagoBetweenAndEstadoPago(inicioDia,    finDia,    "PAGADO");
        List<PagoPedido> pagosSemana = pagoPedidoRepository.findByFechaPagoBetweenAndEstadoPago(inicioSemana, finSemana, "PAGADO");
        List<PagoPedido> pagosMes    = pagoPedidoRepository.findByFechaPagoBetweenAndEstadoPago(inicioMes,    finMes,    "PAGADO");
 
        // ── KPIs ─────────────────────────────────────────────────────────────
        double kpiHoy       = sumaMonto(pagosHoy);
        double kpiHoyCaja   = sumaMontoMetodo(pagosHoy,    "CAJA");
        double kpiHoyLinea  = sumaMontoMetodo(pagosHoy,    "LINEA");
 
        double kpiSemana      = sumaMonto(pagosSemana);
        double kpiSemanaCaja  = sumaMontoMetodo(pagosSemana, "CAJA");
        double kpiSemanaLinea = sumaMontoMetodo(pagosSemana, "LINEA");
 
        double kpiMes      = sumaMonto(pagosMes);
        double kpiMesCaja  = sumaMontoMetodo(pagosMes, "CAJA");
        double kpiMesLinea = sumaMontoMetodo(pagosMes, "LINEA");
 
        // ── Gráfica HOY: ingresos por hora (07:00–23:00) ─────────────────────
        Map<Integer, Double> porHora = agruparPorHora(pagosHoy);
        List<ChartDataDTO> chartHoy = new ArrayList<>();
        for (int h = 7; h <= 23; h++) {
            chartHoy.add(new ChartDataDTO(
                String.format("%02d:00", h),
                porHora.getOrDefault(h, 0.0)
            ));
        }
 
        // ── Gráfica SEMANA: ingresos por día (Lun–Dom) ───────────────────────
        Map<Integer, Double> porDiaSemana = agruparPorDiaSemana(pagosSemana);
        String[] diasSemana = {"", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        List<ChartDataDTO> chartSemana = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            chartSemana.add(new ChartDataDTO(diasSemana[d], porDiaSemana.getOrDefault(d, 0.0)));
        }
 
        // ── Gráfica MES: ingresos por día del mes (1–hoy) ────────────────────
        Map<Integer, Double> porDiaMes = agruparPorDiaMes(pagosMes);
        List<ChartDataDTO> chartMes = new ArrayList<>();
        for (int d = 1; d <= hoy.getDayOfMonth(); d++) {
            chartMes.add(new ChartDataDTO(String.valueOf(d), porDiaMes.getOrDefault(d, 0.0)));
        }
 
        return new DashboardFinancieroDTO(
            kpiHoy,    kpiHoyCaja,    kpiHoyLinea,
            kpiSemana, kpiSemanaCaja, kpiSemanaLinea,
            kpiMes,    kpiMesCaja,    kpiMesLinea,
            chartHoy, chartSemana, chartMes
        );
    }

    // ── Dashboard de Platos ───────────────────────────────────────────────────

    public DashboardPlatosDTO obtenerDatosPlatos() {
        LocalDate hoy = LocalDate.now();

        // ── Ventas por plato ──────────────────────────────────────────────────
        List<Object[]> rawPlatos = pedidoDetalleRepository.ventasPorPlatoEnFecha(hoy);

        long totalUnidades = rawPlatos.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<PlatoVentaDTO> platosDia = rawPlatos.stream()
                .map(r -> {
                    String nombre   = (String)  r[0];
                    long   cantidad = ((Number) r[1]).longValue();
                    double ingresos = ((Number) r[2]).doubleValue();
                    double pct      = totalUnidades > 0
                            ? Math.round((cantidad * 100.0 / totalUnidades) * 10.0) / 10.0
                            : 0.0;
                    return new PlatoVentaDTO(nombre, cantidad, pct, ingresos);
                })
                .collect(Collectors.toList());

        // ── Evolución de unidades por hora ────────────────────────────────────
        List<Object[]> rawHoras = pedidoDetalleRepository.unidadesPorHoraEnFecha(hoy);

        Map<Integer, Long> unidadesPorHora = rawHoras.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).intValue(),
                        r -> ((Number) r[1]).longValue()));

        List<ChartDataDTO> ventasPorHora = new ArrayList<>();
        for (int i = 12; i <= 23; i++) {
            ventasPorHora.add(new ChartDataDTO(
                    i + ":00",
                    unidadesPorHora.getOrDefault(i, 0L).doubleValue()));
        }

        // ── Ventas por categoría ──────────────────────────────────────────────
        List<Object[]> rawCategorias = pedidoDetalleRepository.ventasPorCategoriaEnFecha(hoy);

        long totalUniCat = rawCategorias.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<CategoriaVentaDTO> categorias = rawCategorias.stream()
                .map(r -> {
                    String nombre   = (String)  r[0];
                    long   cantidad = ((Number) r[1]).longValue();
                    double ingresos = ((Number) r[2]).doubleValue();
                    double pct      = totalUniCat > 0
                            ? Math.round((cantidad * 100.0 / totalUniCat) * 10.0) / 10.0
                            : 0.0;
                    return new CategoriaVentaDTO(nombre, cantidad, pct, ingresos);
                })
                .collect(Collectors.toList());

        return new DashboardPlatosDTO(ventasPorHora, platosDia, categorias, totalUnidades);
    }

    // ── Dashboard de Clientes ─────────────────────────────────────────────────

    public DashboardClientesDTO obtenerDatosClientes() {
        LocalDate hoy        = LocalDate.now();
        LocalDate inicioSem  = hoy.with(DayOfWeek.MONDAY);
        LocalDate finSem     = hoy.with(DayOfWeek.SUNDAY);

        // ── KPIs ─────────────────────────────────────────────────────────────
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes    = hoy.withDayOfMonth(hoy.lengthOfMonth());

        Long pedidosHoy     = pedidoRepository.countByFechaPedido(hoy);
        Long reservasHoy    = reservaRepository.countByFecha(hoy);

        // Pedidos de la semana: suma de todos los días
        Long pedidosSemana  = pedidoRepository
                .countAgrupadoPorDiaSemana(inicioSem, finSem)
                .stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        Long reservasSemana = reservaRepository
                .countAgrupadoPorDiaSemana(inicioSem, finSem)
                .stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        Long pedidosMes  = pedidoRepository.countByFechaPedidoBetween(inicioMes, finMes);
        Long reservasMes = reservaRepository.countByFechaBetween(inicioMes, finMes);

        // ── Gráficas por día de semana ────────────────────────────────────────
        // DAYOFWEEK en MySQL: 1=Dom, 2=Lun, 3=Mar, 4=Mié, 5=Jue, 6=Vie, 7=Sáb
        // Lo remapeamos a Lun(1)..Dom(7) para el frontend
        String[] dias = {"", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        Map<Integer, Long> pedidosPorDia = pedidoRepository
                .countAgrupadoPorDiaSemana(inicioSem, finSem)
                .stream()
                .collect(Collectors.toMap(
                        r -> remapDayOfWeek(((Number) r[0]).intValue()),
                        r -> ((Number) r[1]).longValue()));

        Map<Integer, Long> reservasPorDia = reservaRepository
                .countAgrupadoPorDiaSemana(inicioSem, finSem)
                .stream()
                .collect(Collectors.toMap(
                        r -> remapDayOfWeek(((Number) r[0]).intValue()),
                        r -> ((Number) r[1]).longValue()));

        List<ChartDataDTO> chartPedidos  = new ArrayList<>();
        List<ChartDataDTO> chartReservas = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            chartPedidos.add(new ChartDataDTO(dias[i],
                    pedidosPorDia.getOrDefault(i, 0L).doubleValue()));
            chartReservas.add(new ChartDataDTO(dias[i],
                    reservasPorDia.getOrDefault(i, 0L).doubleValue()));
        }

        // ── Distribución de reservas por estado (semana) ────────────────────
        List<Object[]> rawEstados = reservaRepository.countPorEstadoEnSemana(inicioSem, finSem);

        // Garantizamos que los 3 estados siempre aparecen aunque sean 0
        Map<String, Long> estadoMap = new java.util.LinkedHashMap<>();
        estadoMap.put("PENDIENTE", 0L);
        estadoMap.put("PAGADA",    0L);
        estadoMap.put("CANCELADA", 0L);
        rawEstados.forEach(r -> estadoMap.put((String) r[0], ((Number) r[1]).longValue()));

        List<ChartDataDTO> reservasPorEstado = estadoMap.entrySet().stream()
                .map(e -> new ChartDataDTO(e.getKey(), e.getValue().doubleValue()))
                .collect(Collectors.toList());

        return new DashboardClientesDTO(
                pedidosHoy, pedidosSemana, pedidosMes,
                reservasHoy, reservasSemana, reservasMes,
                chartPedidos, chartReservas, reservasPorEstado);
    }

    public CierreCajaDTO obtenerCierreCaja() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia    = hoy.atTime(LocalTime.MAX);
 
        List<PagoPedido> pagosHoy = pagoPedidoRepository
            .findByFechaPagoBetweenAndEstadoPago(inicioDia, finDia, "PAGADO");
 
        List<PagoPedido> caja  = pagosHoy.stream()
            .filter(p -> "CAJA".equalsIgnoreCase(p.getMetodoPago()))
            .collect(Collectors.toList());
 
        List<PagoPedido> linea = pagosHoy.stream()
            .filter(p -> "LINEA".equalsIgnoreCase(p.getMetodoPago()))
            .collect(Collectors.toList());
 
        return new CierreCajaDTO(
            hoy,
            sumaMonto(pagosHoy),
            sumaMonto(caja),
            sumaMonto(linea),
            pagosHoy.size(),
            caja.size(),
            linea.size()
        );
    }

    /**
     * MySQL DAYOFWEEK: 1=Dom, 2=Lun ... 7=Sáb
     * Lo convertimos a:  1=Lun, 2=Mar ... 7=Dom
     */
    private int remapDayOfWeek(int mysqlDay) {
        return mysqlDay == 1 ? 7 : mysqlDay - 1;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private double sumaMonto(List<PagoPedido> pagos) {
        return pagos.stream().mapToDouble(PagoPedido::getMonto).sum();
    }
 
    private double sumaMontoMetodo(List<PagoPedido> pagos, String metodo) {
        return pagos.stream()
            .filter(p -> metodo.equalsIgnoreCase(p.getMetodoPago()))
            .mapToDouble(PagoPedido::getMonto)
            .sum();
    }
 
    private Map<Integer, Double> agruparPorHora(List<PagoPedido> pagos) {
        return pagos.stream().collect(
            Collectors.groupingBy(
                p -> p.getFechaPago().getHour(),
                Collectors.summingDouble(PagoPedido::getMonto)
            )
        );
    }
 
    private Map<Integer, Double> agruparPorDiaSemana(List<PagoPedido> pagos) {
        // DayOfWeek.getValue(): 1=Lun … 7=Dom
        return pagos.stream().collect(
            Collectors.groupingBy(
                p -> p.getFechaPago().getDayOfWeek().getValue(),
                Collectors.summingDouble(PagoPedido::getMonto)
            )
        );
    }
 
    private Map<Integer, Double> agruparPorDiaMes(List<PagoPedido> pagos) {
        return pagos.stream().collect(
            Collectors.groupingBy(
                p -> p.getFechaPago().getDayOfMonth(),
                Collectors.summingDouble(PagoPedido::getMonto)
            )
        );
    }
}