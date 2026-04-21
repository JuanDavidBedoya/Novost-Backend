package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

// Servicio de métricas para monitorear creación de pedidos (RNF-07: desempeño y confiabilidad)

@Service
public class PedidoMetricaService {

// Atributos: contadores para intentos, éxitos y fallos, y timer para tiempo de procesamiento

    private final Counter intentosTotales;
    private final Counter pedidosExitosos;
    private final Counter pedidosFallidos;
    private final Timer   tiempoProcesamiento;

    // Constructor: inicializa contadores y timer en el registry con tags de RNF-07

    public PedidoMetricaService(MeterRegistry registry) {

        this.intentosTotales = Counter.builder("pedidos_intentos_total")
                .description("Total de intentos de creación de pedidos")
                .tag("rnf", "RNF-07")
                .register(registry);

        this.pedidosExitosos = Counter.builder("pedidos_exitosos_total")
                .description("Total de pedidos creados exitosamente")
                .tag("rnf", "RNF-07")
                .register(registry);

        this.pedidosFallidos = Counter.builder("pedidos_fallidos_total")
                .description("Total de pedidos que fallaron al crearse")
                .tag("rnf", "RNF-07")
                .register(registry);

        // Timer para medir cuánto tarda en procesarse cada pedido
        this.tiempoProcesamiento = Timer.builder("pedidos_tiempo_procesamiento")
                .description("Tiempo de procesamiento de creación de pedido")
                .tag("rnf", "RNF-07")
                .register(registry);
    }

    // Métodos de registro: incrementan contadores y exponen timer para medir duración

    public void registrarIntento()  { intentosTotales.increment(); }
    public void registrarExito()    { pedidosExitosos.increment(); }
    public void registrarFallo()    { pedidosFallidos.increment(); }
    public Timer getTiempoTimer()   { return tiempoProcesamiento; }

    // Métodos getter: retornan valores totales de intentos, éxitos, fallos y acceso al timer

    public double getIntentos()  { return intentosTotales.count(); }
    public double getExitosos()  { return pedidosExitosos.count(); }
    public double getFallidos()  { return pedidosFallidos.count(); }
}