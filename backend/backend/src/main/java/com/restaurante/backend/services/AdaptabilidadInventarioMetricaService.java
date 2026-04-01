package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AdaptabilidadInventarioMetricaService {

    private final Counter productosCreados_total;
    private final Counter productosCreadosKg_total;
    private final Counter productosCreadosLitro_total;
    private final Counter productosCreadosUnidad_total;

    // Mapa semana -> cantidad creada esa semana (clave: "yyyy-Www")
    private final ConcurrentHashMap<String, AtomicLong> productosPorSemana
            = new ConcurrentHashMap<>();

    // Gauge: productos creados en la semana actual
    private final AtomicLong productosSemanaActual = new AtomicLong(0);

    // Semana en curso (para detectar cambio de semana)
    private volatile String semanaActual = semanaActual();

    public AdaptabilidadInventarioMetricaService(MeterRegistry registry) {

        this.productosCreados_total = Counter.builder("novost_inventario_productos_creados_total")
                .description("Total de productos nuevos creados en inventario")
                .tag("rnf", "RNF-20")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        this.productosCreadosKg_total = Counter.builder("novost_inventario_productos_creados_por_unidad_total")
                .description("Productos nuevos creados con unidad KG")
                .tag("rnf", "RNF-20")
                .tag("proceso", "gestion_inventario")
                .tag("unidad", "KG")
                .register(registry);

        this.productosCreadosLitro_total = Counter.builder("novost_inventario_productos_creados_por_unidad_total")
                .description("Productos nuevos creados con unidad L")
                .tag("rnf", "RNF-20")
                .tag("proceso", "gestion_inventario")
                .tag("unidad", "L")
                .register(registry);

        this.productosCreadosUnidad_total = Counter.builder("novost_inventario_productos_creados_por_unidad_total")
                .description("Productos nuevos creados con unidad UND")
                .tag("rnf", "RNF-20")
                .tag("proceso", "gestion_inventario")
                .tag("unidad", "UND")
                .register(registry);

        // Gauge: productos creados en la semana en curso
        Gauge.builder("novost_inventario_productos_creados_semana_actual",
                        productosSemanaActual,
                        AtomicLong::get)
                .description("Productos nuevos creados durante la semana en curso (crecimiento semanal)")
                .tag("rnf", "RNF-20")
                .register(registry);
    }

    /**
     * Registrar la creación de un nuevo producto.
     * @param tipoMedida "KILO", "LITRO" o "UNIDAD"
     */
    public void registrarProductoCreado(String tipoMedida) {
        productosCreados_total.increment();

        // Contador por unidad de medida
        switch (tipoMedida) {
            case "KILO"   -> productosCreadosKg_total.increment();
            case "LITRO"  -> productosCreadosLitro_total.increment();
            case "UNIDAD" -> productosCreadosUnidad_total.increment();
        }

        // Detectar si cambió la semana y reiniciar el contador semanal
        String semanaAhora = semanaActual();
        if (!semanaAhora.equals(semanaActual)) {
            semanaActual = semanaAhora;
            productosSemanaActual.set(0);
        }

        // Acumular en el mapa por semana
        productosPorSemana
                .computeIfAbsent(semanaAhora, k -> new AtomicLong(0))
                .incrementAndGet();

        // Actualizar el gauge de la semana actual
        productosSemanaActual.incrementAndGet();
    }

    /** Clave de semana en formato "yyyy-Www" (ej: "2026-W13") */
    private String semanaActual() {
        LocalDate hoy   = LocalDate.now();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return lunes.getYear() + "-W"
                + String.format("%02d", lunes.get(
                        java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
    }

    public long getTotalCreados()        { return (long) productosCreados_total.count(); }
    public long getProductosSemanaActual(){ return productosSemanaActual.get(); }
    public ConcurrentHashMap<String, AtomicLong> getHistorialSemanal() {
        return productosPorSemana;
    }
}
