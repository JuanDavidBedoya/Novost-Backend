package com.restaurante.backend.services;

import com.restaurante.backend.dtos.*;
import java.time.DayOfWeek;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.PagoPedido;
import com.restaurante.backend.repositories.PagoRepository;
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

    private final PagoRepository           pagoRepository;
    private final PagoPedidoRepository     pagoPedidoRepository;
    private final PedidoDetalleRepository  pedidoDetalleRepository;
    private final PedidoRepository          pedidoRepository;
    private final ReservaRepository         reservaRepository;

    // ── Dashboard Financiero ──────────────────────────────────────────────────

    public DashboardFinancieroDTO obtenerDatosFinancieros() {
        LocalDate hoy = LocalDate.now();

        LocalDateTime inicioDia    = hoy.atStartOfDay();
        LocalDateTime finDia       = hoy.atTime(LocalTime.MAX);
        LocalDateTime inicioSemana = hoy.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime finSemana    = hoy.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        LocalDateTime inicioAnio   = hoy.withDayOfYear(1).atStartOfDay();
        LocalDateTime finAnio      = hoy.atTime(LocalTime.MAX);

        List<Pago>       reservasHoy    = filtrarReservas(pagoRepository.findByFechaPagoBetween(inicioDia,    finDia));
        List<Pago>       reservasSemana = filtrarReservas(pagoRepository.findByFechaPagoBetween(inicioSemana, finSemana));
        List<Pago>       reservasAnio   = filtrarReservas(pagoRepository.findByFechaPagoBetween(inicioAnio,   finAnio));
        List<PagoPedido> pedidosHoy     = filtrarPedidos(pagoPedidoRepository.findByFechaPagoBetween(inicioDia,    finDia));
        List<PagoPedido> pedidosSemana  = filtrarPedidos(pagoPedidoRepository.findByFechaPagoBetween(inicioSemana, finSemana));
        List<PagoPedido> pedidosAnio    = filtrarPedidos(pagoPedidoRepository.findByFechaPagoBetween(inicioAnio,   finAnio));

        double kpiHoy    = sumaReservas(reservasHoy)    + sumaPedidos(pedidosHoy);
        double kpiSemana = sumaReservas(reservasSemana) + sumaPedidos(pedidosSemana);
        double kpiTotal  = sumaReservas(reservasAnio)   + sumaPedidos(pedidosAnio);

        // Gráfica HOY por hora
        Map<Integer, Double> porHoraRes = agruparReservasPorHora(reservasHoy);
        Map<Integer, Double> porHoraPed = agruparPedidosPorHora(pedidosHoy);
        List<ChartDataDTO> chartHoy = new ArrayList<>();
        for (int i = 12; i <= 23; i++) {
            chartHoy.add(new ChartDataDTO(i + ":00",
                    porHoraRes.getOrDefault(i, 0.0) + porHoraPed.getOrDefault(i, 0.0)));
        }

        // Gráfica SEMANA por día
        Map<Integer, Double> porDiaRes = agruparReservasPorDia(reservasSemana);
        Map<Integer, Double> porDiaPed = agruparPedidosPorDia(pedidosSemana);
        List<ChartDataDTO> chartSemana = new ArrayList<>();
        String[] dias = {"", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        for (int i = 1; i <= 7; i++) {
            chartSemana.add(new ChartDataDTO(dias[i],
                    porDiaRes.getOrDefault(i, 0.0) + porDiaPed.getOrDefault(i, 0.0)));
        }

        // Gráfica AÑO por mes
        Map<Integer, Double> porMesRes = agruparReservasPorMes(reservasAnio);
        Map<Integer, Double> porMesPed = agruparPedidosPorMes(pedidosAnio);
        List<ChartDataDTO> chartMeses = new ArrayList<>();
        String[] meses = {"", "Ene", "Feb", "Mar", "Abr", "May", "Jun",
                               "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        for (int i = 1; i <= hoy.getMonthValue(); i++) {
            chartMeses.add(new ChartDataDTO(meses[i],
                    porMesRes.getOrDefault(i, 0.0) + porMesPed.getOrDefault(i, 0.0)));
        }

        return new DashboardFinancieroDTO(kpiHoy, kpiSemana, kpiTotal,
                chartHoy, chartSemana, chartMeses);
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

    /**
     * MySQL DAYOFWEEK: 1=Dom, 2=Lun ... 7=Sáb
     * Lo convertimos a:  1=Lun, 2=Mar ... 7=Dom
     */
    private int remapDayOfWeek(int mysqlDay) {
        return mysqlDay == 1 ? 7 : mysqlDay - 1;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private List<Pago> filtrarReservas(List<Pago> pagos) {
        return pagos.stream()
                .filter(p -> "succeeded".equalsIgnoreCase(p.getEstadoPago()))
                .collect(Collectors.toList());
    }

    private List<PagoPedido> filtrarPedidos(List<PagoPedido> pagos) {
        return pagos.stream()
                .filter(p -> "PAGADO".equalsIgnoreCase(p.getEstadoPago()))
                .collect(Collectors.toList());
    }

    private double sumaReservas(List<Pago> p)       { return p.stream().mapToDouble(Pago::getMonto).sum(); }
    private double sumaPedidos(List<PagoPedido> p)   { return p.stream().mapToDouble(PagoPedido::getMonto).sum(); }

    private Map<Integer, Double> agruparReservasPorHora(List<Pago> p) {
        return p.stream().collect(Collectors.groupingBy(
                x -> x.getFechaPago().getHour(), Collectors.summingDouble(Pago::getMonto)));
    }
    private Map<Integer, Double> agruparPedidosPorHora(List<PagoPedido> p) {
        return p.stream().collect(Collectors.groupingBy(
                x -> x.getFechaPago().getHour(), Collectors.summingDouble(PagoPedido::getMonto)));
    }
    private Map<Integer, Double> agruparReservasPorDia(List<Pago> p) {
        return p.stream().collect(Collectors.groupingBy(
                x -> x.getFechaPago().getDayOfWeek().getValue(), Collectors.summingDouble(Pago::getMonto)));
    }
    private Map<Integer, Double> agruparPedidosPorDia(List<PagoPedido> p) {
        return p.stream().collect(Collectors.groupingBy(
                x -> x.getFechaPago().getDayOfWeek().getValue(), Collectors.summingDouble(PagoPedido::getMonto)));
    }
    private Map<Integer, Double> agruparReservasPorMes(List<Pago> p) {
        return p.stream().collect(Collectors.groupingBy(
                x -> x.getFechaPago().getMonthValue(), Collectors.summingDouble(Pago::getMonto)));
    }
    private Map<Integer, Double> agruparPedidosPorMes(List<PagoPedido> p) {
        return p.stream().collect(Collectors.groupingBy(
                x -> x.getFechaPago().getMonthValue(), Collectors.summingDouble(PagoPedido::getMonto)));
    }
}