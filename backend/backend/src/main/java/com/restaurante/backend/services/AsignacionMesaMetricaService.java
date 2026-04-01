package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AsignacionMesaMetricaService {

    private final Timer tiempoAsignacion;
    private final Counter asignacionesTotal;
    private final Counter asignacionesLentasTotal; // superan 500ms

    // Para el Gauge de tasa de asignaciones lentas
    private final AtomicLong totalAsignaciones  = new AtomicLong(0);
    private final AtomicLong asignacionesLentas = new AtomicLong(0);

    // Peor tiempo registrado (para análisis)
    private final AtomicReference<Double> peorTiempoMs = new AtomicReference<>(0.0);

    public AsignacionMesaMetricaService(MeterRegistry registry) {

        // Timer: registra distribución completa de tiempos (avg, max, p95, p99)
        this.tiempoAsignacion = Timer.builder("novost_reserva_asignacion_mesa_duracion")
                .description("Tiempo de ejecución del algoritmo de asignación automática de mesa")
                .tag("rnf", "RNF-14")
                .tag("proceso", "reserva_mesas")
                .publishPercentiles(0.95, 0.99)   // expone P95 y P99 en Prometheus
                .publishPercentileHistogram(true)  // permite calcular percentiles en Grafana
                .register(registry);

        this.asignacionesTotal = Counter.builder("novost_reserva_asignaciones_total")
                .description("Total de asignaciones automáticas de mesa ejecutadas")
                .tag("rnf", "RNF-14")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        this.asignacionesLentasTotal = Counter.builder("novost_reserva_asignaciones_lentas_total")
                .description("Asignaciones que superaron 500ms (incumplen RNF-14)")
                .tag("rnf", "RNF-14")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        // Gauge: porcentaje de asignaciones lentas
        Gauge.builder("novost_reserva_asignaciones_lentas_porcentaje",
                        this,
                        svc -> {
                            long total = svc.totalAsignaciones.get();
                            if (total == 0) return 0.0;
                            return (svc.asignacionesLentas.get() * 100.0) / total;
                        })
                .description("Porcentaje de asignaciones que superaron 500ms")
                .tag("rnf", "RNF-14")
                .register(registry);

        // Gauge: peor tiempo registrado en ms
        Gauge.builder("novost_reserva_asignacion_peor_tiempo_ms",
                        peorTiempoMs,
                        AtomicReference::get)
                .description("Peor tiempo de asignación registrado en milisegundos")
                .tag("rnf", "RNF-14")
                .register(registry);
    }

    /**
     * Registra una asignación con su duración en milisegundos.
     * Llamar DESPUÉS de que el algoritmo termine.
     */
    public void registrarAsignacion(long duracionMs) {
        // Registrar en el Timer (convierte ms a nanosegundos)
        tiempoAsignacion.record(duracionMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        asignacionesTotal.increment();
        totalAsignaciones.incrementAndGet();

        // Actualizar peor tiempo
        peorTiempoMs.updateAndGet(actual -> Math.max(actual, (double) duracionMs));

        // Verificar si supera el umbral de 500ms
        if (duracionMs > 500) {
            asignacionesLentasTotal.increment();
            asignacionesLentas.incrementAndGet();
        }
    }

    public Timer  getTimer()             { return tiempoAsignacion; }
    public long   getTotalAsignaciones() { return totalAsignaciones.get(); }
    public long   getAsignacionesLentas(){ return asignacionesLentas.get(); }
    public double getPeorTiempoMs()      { return peorTiempoMs.get(); }
    public double getPromedioMs() {
        double mean = tiempoAsignacion.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
        return Double.isNaN(mean) ? 0.0 : mean;
    }
    public double getTasaLentas() {
        long total = totalAsignaciones.get();
        if (total == 0) return 0.0;
        return (asignacionesLentas.get() * 100.0) / total;
    }
}