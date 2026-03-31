package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class RedirectMetricaService {

    private final Counter totalRedireccionamientos;
    private final Counter rendimientoBajo;
    private final DistributionSummary duracionSummary;

    public RedirectMetricaService(MeterRegistry registry) {
        this.totalRedireccionamientos = Counter.builder("novost_redirect_menu_pedidos_total")
                .description("Total de redireccionamientos del menú a pedidos")
                .register(registry);

        this.rendimientoBajo = Counter.builder("novost_redirect_menu_pedidos_rendimiento_bajo_total")
                .description("Redireccionamientos que superaron 0.5 segundos")
                .register(registry);

        // ✅ NUEVO: registra la distribución de tiempos en segundos
        this.duracionSummary = DistributionSummary.builder("novost_redirect_menu_pedidos_duracion_segundos")
                .description("Duración de cada redireccionamiento menú → pedidos en segundos")
                .baseUnit("segundos")
                .register(registry);
    }

    public void registrar(double duracionSegundos) {
        totalRedireccionamientos.increment();
        duracionSummary.record(duracionSegundos);   // ✅ NUEVO
        if (duracionSegundos > 0.5) {
            rendimientoBajo.increment();
        }
    }

    public double getTotal()              { return totalRedireccionamientos.count(); }
    public double getRendimientoBajo()    { return rendimientoBajo.count(); }
    public double getTiempoPromedio()     { return duracionSummary.mean(); }       // ✅ NUEVO
    public double getTiempoMaximo()       { return duracionSummary.max(); }        // ✅ NUEVO
    public double getTotalDuracion()      { return duracionSummary.totalAmount(); } // ✅ NUEVO
}