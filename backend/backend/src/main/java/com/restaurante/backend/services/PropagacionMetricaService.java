package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

// Servicio de métricas para monitorear propagación de cambios de estado de platos (RNF-19: reactividad)

@Service
public class PropagacionMetricaService {

    // Atributos: contadores de propagaciones totales y lentas, summary de duraciones y máximo histórico

    private final Counter totalPropagaciones;
    private final Counter propagacionesLentas;      // superan 3 segundos
    private final DistributionSummary duracionSummary;
    private volatile double maximoHistorico = 0.0;

    // Constructor: inicializa contadores y distribution summary en el registry para tracking de RNF-19

    public PropagacionMetricaService(MeterRegistry registry) {
        this.totalPropagaciones = Counter.builder("novost_rnf19_propagacion_total")
                .description("Total de propagaciones de cambio de estado de plato")
                .register(registry);

        this.propagacionesLentas = Counter.builder("novost_rnf19_propagacion_lenta_total")
                .description("Propagaciones que superaron 3 segundos")
                .register(registry);

        this.duracionSummary = DistributionSummary
                .builder("novost_rnf19_propagacion_duracion_segundos")
                .description("Tiempo de propagación del cambio de estado al menú del cliente")
                .baseUnit("segundos")
                .register(registry);
    }

    // Método registrar: incrementa contadores, registra duración, detecta propagaciones lentas (>3s) y máximo

    public void registrar(double duracionSegundos) {
        totalPropagaciones.increment();
        duracionSummary.record(duracionSegundos);
        if (duracionSegundos > maximoHistorico) maximoHistorico = duracionSegundos;
        if (duracionSegundos > 3.0) propagacionesLentas.increment();
    }

    // Métodos getter: retornan total de propagaciones, lentas, promedio de duración y máximo histórico

    public double getTotal()               { return totalPropagaciones.count(); }
    public double getPropagacionesLentas() { return propagacionesLentas.count(); }
    public double getTiempoPromedio()      { return duracionSummary.mean(); }
    public double getTiempoMaximo()        { return maximoHistorico; }
}