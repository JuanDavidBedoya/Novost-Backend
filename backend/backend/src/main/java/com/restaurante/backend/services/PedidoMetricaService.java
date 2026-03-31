package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class PedidoMetricaService {

    private final Counter intentosTotales;
    private final Counter pedidosExitosos;
    private final Counter pedidosFallidos;
    private final Timer   tiempoProcesamiento;

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

        // ✅ Timer para medir cuánto tarda en procesarse cada pedido
        this.tiempoProcesamiento = Timer.builder("pedidos_tiempo_procesamiento")
                .description("Tiempo de procesamiento de creación de pedido")
                .tag("rnf", "RNF-07")
                .register(registry);
    }

    public void registrarIntento()  { intentosTotales.increment(); }
    public void registrarExito()    { pedidosExitosos.increment(); }
    public void registrarFallo()    { pedidosFallidos.increment(); }
    public Timer getTiempoTimer()   { return tiempoProcesamiento; }

    public double getIntentos()  { return intentosTotales.count(); }
    public double getExitosos()  { return pedidosExitosos.count(); }
    public double getFallidos()  { return pedidosFallidos.count(); }
}