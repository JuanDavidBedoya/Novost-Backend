package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ModularidadInventarioMetricaService {

    // Servicio de métricas para monitorear actualizaciones de inventario (RNF-16: desempeño)

    private final Timer tiempoActualizacion;
    private final Counter actualizacionesTotal;
    private final Counter actualizacionesLentasTotal; // superan 300ms

    // Atributos: timers, contadores y referencias atómicas para registrar métricas de actualización

    private final AtomicLong totalActualizaciones  = new AtomicLong(0);
    private final AtomicLong actualizacionesLentas = new AtomicLong(0);
    private final AtomicReference<Double> peorTiempoMs = new AtomicReference<>(0.0);

    public ModularidadInventarioMetricaService(MeterRegistry registry) {

        this.tiempoActualizacion = Timer.builder("novost_inventario_actualizacion_duracion")
                .description("Tiempo de respuesta del update de producto en inventario")
                .tag("rnf", "RNF-16")
                .tag("proceso", "gestion_inventario")
                .publishPercentiles(0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(registry);

        this.actualizacionesTotal = Counter.builder("novost_inventario_actualizaciones_total")
                .description("Total de actualizaciones de productos ejecutadas")
                .tag("rnf", "RNF-16")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        this.actualizacionesLentasTotal = Counter.builder("novost_inventario_actualizaciones_lentas_total")
                .description("Actualizaciones que superaron 300ms (incumplen RNF-16)")
                .tag("rnf", "RNF-16")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        // Gauge: porcentaje de actualizaciones lentas
        Gauge.builder("novost_inventario_actualizaciones_lentas_porcentaje",
                        this,
                        svc -> {
                            long total = svc.totalActualizaciones.get();
                            if (total == 0) return 0.0;
                            return (svc.actualizacionesLentas.get() * 100.0) / total;
                        })
                .description("Porcentaje de actualizaciones que superaron 300ms")
                .tag("rnf", "RNF-16")
                .register(registry);

        // Gauge: peor tiempo registrado
        Gauge.builder("novost_inventario_actualizacion_peor_tiempo_ms",
                        peorTiempoMs,
                        AtomicReference::get)
                .description("Peor tiempo de actualización de producto registrado en ms")
                .tag("rnf", "RNF-16")
                .register(registry);
    }

    // Método registrarActualizacion: registra duración de actualización, incrementa contadores y detecta actualizaciones lentas (>300ms)

    public void registrarActualizacion(long duracionMs) {
        tiempoActualizacion.record(duracionMs, TimeUnit.MILLISECONDS);

        actualizacionesTotal.increment();
        totalActualizaciones.incrementAndGet();

        peorTiempoMs.updateAndGet(actual -> Math.max(actual, (double) duracionMs));

        if (duracionMs > 300) {
            actualizacionesLentasTotal.increment();
            actualizacionesLentas.incrementAndGet();
        }
    }

    // Métodos getter: retornan totales, lentas, peor tiempo, promedio en ms y tasa de lentas (porcentaje)

    public long   getTotalActualizaciones()  { return totalActualizaciones.get(); }
    public long   getActualizacionesLentas() { return actualizacionesLentas.get(); }
    public double getPeorTiempoMs()          { return peorTiempoMs.get(); }
    public double getPromedioMs() {
        double mean = tiempoActualizacion.mean(TimeUnit.MILLISECONDS);
        return Double.isNaN(mean) ? 0.0 : mean;
    }
    public double getTasaLentas() {
        long total = totalActualizaciones.get();
        if (total == 0) return 0.0;
        return (actualizacionesLentas.get() * 100.0) / total;
    }
}
