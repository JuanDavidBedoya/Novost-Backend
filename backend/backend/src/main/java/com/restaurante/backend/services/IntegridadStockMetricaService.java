package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class IntegridadStockMetricaService {

    private final Counter consumosIntentadosTotal;
    private final Counter consumosAceptadosTotal;
    private final Counter consumosRechazadosTotal;

    // Para el Gauge de tasa de rechazo
    private final AtomicLong intentados = new AtomicLong(0);
    private final AtomicLong rechazados = new AtomicLong(0);

    public IntegridadStockMetricaService(MeterRegistry registry) {

        this.consumosIntentadosTotal = Counter.builder("novost_stock_consumos_intentados_total")
                .description("Total de intentos de consumo (quitar stock) de inventario")
                .tag("rnf", "RNF-12")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        this.consumosAceptadosTotal = Counter.builder("novost_stock_consumos_aceptados_total")
                .description("Consumos de stock aceptados correctamente")
                .tag("rnf", "RNF-12")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        this.consumosRechazadosTotal = Counter.builder("novost_stock_consumos_rechazados_total")
                .description("Consumos rechazados por validación (stock insuficiente o cantidad inválida)")
                .tag("rnf", "RNF-12")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        // Gauge: tasa de rechazo en % (objetivo < 5%)
        Gauge.builder("novost_stock_tasa_rechazo_porcentaje",
                        this,
                        svc -> {
                            long total = svc.intentados.get();
                            if (total == 0) return 0.0;
                            return (svc.rechazados.get() * 100.0) / total;
                        })
                .description("Tasa de consumos rechazados por validación (objetivo < 5%)")
                .tag("rnf", "RNF-12")
                .register(registry);
    }

    public void registrarIntento() {
        consumosIntentadosTotal.increment();
        intentados.incrementAndGet();
    }

    public void registrarAceptado() {
        consumosAceptadosTotal.increment();
    }

    public void registrarRechazado() {
        consumosRechazadosTotal.increment();
        rechazados.incrementAndGet();
    }

    public long   getIntentados()  { return intentados.get(); }
    public long   getRechazados()  { return rechazados.get(); }
    public double getTasaRechazo() {
        if (intentados.get() == 0) return 0.0;
        return (rechazados.get() * 100.0) / intentados.get();
    }
}
