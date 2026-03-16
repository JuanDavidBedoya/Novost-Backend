package com.restaurante.backend.services;

import com.restaurante.backend.dtos.ChartDataDTO;
import com.restaurante.backend.dtos.DashboardFinancieroDTO;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.repositories.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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

    private final PagoRepository pagoRepository;

    public DashboardFinancieroDTO obtenerDatosFinancieros() {
        LocalDate hoy = LocalDate.now();
        
        // Rangos de tiempo
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);
        LocalDateTime inicioSemana = hoy.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime finSemana = hoy.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        LocalDateTime inicioAnio = hoy.withDayOfYear(1).atStartOfDay();

        // Obtener datos de la BD
        List<Pago> pagosHoy = pagoRepository.findByFechaPagoBetween(inicioDia, finDia);
        List<Pago> pagosSemana = pagoRepository.findByFechaPagoBetween(inicioSemana, finSemana);
        List<Pago> pagosAnio = pagoRepository.findByFechaPagoBetween(inicioAnio, hoy.atTime(LocalTime.MAX));

        // Filtrar solo pagos exitosos (Ajusta "Aprobado" al estado real que uses en tu BD)
        // Si quieres sumar todo, puedes omitir este .filter()
        pagosHoy = pagosHoy.stream().filter(p -> "succeeded".equalsIgnoreCase(p.getEstadoPago())).toList();
        pagosSemana = pagosSemana.stream().filter(p -> "succeeded".equalsIgnoreCase(p.getEstadoPago())).toList();
        pagosAnio = pagosAnio.stream().filter(p -> "succeeded".equalsIgnoreCase(p.getEstadoPago())).toList();

        // 1. KPIs Totales
        Double kpiHoy = pagosHoy.stream().mapToDouble(Pago::getMonto).sum();
        Double kpiSemana = pagosSemana.stream().mapToDouble(Pago::getMonto).sum();
        Double kpiTotal = pagosAnio.stream().mapToDouble(Pago::getMonto).sum();

        // 2. Gráfica Hoy (Agrupado por Hora)
        Map<Integer, Double> sumaPorHora = pagosHoy.stream()
                .collect(Collectors.groupingBy(p -> p.getFechaPago().getHour(), Collectors.summingDouble(Pago::getMonto)));
        
        List<ChartDataDTO> chartHoy = new ArrayList<>();
        // Mostramos desde las 12 PM hasta las 11 PM (Horario típico de restaurante)
        for (int i = 12; i <= 23; i++) {
            chartHoy.add(new ChartDataDTO(i + ":00", sumaPorHora.getOrDefault(i, 0.0)));
        }

        // 3. Gráfica Semana (Agrupado por Día de la semana)
        Map<Integer, Double> sumaPorDia = pagosSemana.stream()
                .collect(Collectors.groupingBy(p -> p.getFechaPago().getDayOfWeek().getValue(), Collectors.summingDouble(Pago::getMonto)));
        
        List<ChartDataDTO> chartSemana = new ArrayList<>();
        String[] nombresDias = {"", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        for (int i = 1; i <= 7; i++) {
            chartSemana.add(new ChartDataDTO(nombresDias[i], sumaPorDia.getOrDefault(i, 0.0)));
        }

        // 4. Gráfica Año (Agrupado por Mes)
        Map<Integer, Double> sumaPorMes = pagosAnio.stream()
                .collect(Collectors.groupingBy(p -> p.getFechaPago().getMonthValue(), Collectors.summingDouble(Pago::getMonto)));
        
        List<ChartDataDTO> chartMeses = new ArrayList<>();
        String[] nombresMeses = {"", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        for (int i = 1; i <= hoy.getMonthValue(); i++) {
            chartMeses.add(new ChartDataDTO(nombresMeses[i], sumaPorMes.getOrDefault(i, 0.0)));
        }

        return new DashboardFinancieroDTO(kpiHoy, kpiSemana, kpiTotal, chartHoy, chartSemana, chartMeses);
    }
}