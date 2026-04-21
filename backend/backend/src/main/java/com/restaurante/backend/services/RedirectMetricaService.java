package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

// Servicio de métricas para monitorear redireccionamientos menú → pedidos

@Service
public class RedirectMetricaService {

    // Atributos: contadores de redireccionamientos totales y con rendimiento bajo, y distribution summary de duraciones

    private final Counter totalRedireccionamientos;
    private final Counter rendimientoBajo;
    private final DistributionSummary duracionSummary;

    // Constructor: inicializa contadores y distribution summary en el registry para tracking de tiempos de redirect

    public RedirectMetricaService(MeterRegistry registry) {
        this.totalRedireccionamientos = Counter.builder("novost_redirect_menu_pedidos_total")
                .description("Total de redireccionamientos del menú a pedidos")
                .register(registry);

        this.rendimientoBajo = Counter.builder("novost_redirect_menu_pedidos_rendimiento_bajo_total")
                .description("Redireccionamientos que superaron 0.5 segundos")
                .register(registry);

        // Registra la distribución de tiempos en segundos
        this.duracionSummary = DistributionSummary.builder("novost_redirect_menu_pedidos_duracion_segundos")
                .description("Duración de cada redireccionamiento menú → pedidos en segundos")
                .baseUnit("segundos")
                .register(registry);
    }

    // Método registrar: incrementa contadores, registra duración y detecta redireccionamientos lentos (>0.5s)

    public void registrar(double duracionSegundos) {
        totalRedireccionamientos.increment();
        duracionSummary.record(duracionSegundos);  
        if (duracionSegundos > 0.5) {
            rendimientoBajo.increment();
        }
    }

    // Métodos getter: retornan total de redireccionamientos, con rendimiento bajo, tiempo promedio, máximo y duración total

    public double getTotal()              { return totalRedireccionamientos.count(); }
    public double getRendimientoBajo()    { return rendimientoBajo.count(); }
    public double getTiempoPromedio()     { return duracionSummary.mean(); }       
    public double getTiempoMaximo()       { return duracionSummary.max(); }        
    public double getTotalDuracion()      { return duracionSummary.totalAmount(); } 
}