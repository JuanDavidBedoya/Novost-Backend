package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class StockAlertaMetricaService {

    private final Counter alertasIntentadasTotal;
    private final Counter alertasExitosasTotal;
    private final Counter alertasFallidasTotal;

    // Gauge: eficacia en % (se recalcula solo en cada scrape)
    private final AtomicLong intentadas = new AtomicLong(0);
    private final AtomicLong exitosas   = new AtomicLong(0);

    public StockAlertaMetricaService(MeterRegistry registry) {

        this.alertasIntentadasTotal = Counter.builder("novost_stock_alerta_intentadas_total")
                .description("Total de alertas de stock mínimo que debieron enviarse")
                .tag("rnf", "RNF-08")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        this.alertasExitosasTotal = Counter.builder("novost_stock_alerta_exitosas_total")
                .description("Alertas de stock mínimo enviadas correctamente por email")
                .tag("rnf", "RNF-08")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        this.alertasFallidasTotal = Counter.builder("novost_stock_alerta_fallidas_total")
                .description("Alertas de stock mínimo que fallaron al enviarse")
                .tag("rnf", "RNF-08")
                .tag("proceso", "gestion_inventario")
                .register(registry);

        // Gauge: porcentaje de eficacia (exitosas / intentadas * 100)
        Gauge.builder("novost_stock_alerta_eficacia_porcentaje",
                        this,
                        svc -> {
                            long total = svc.intentadas.get();
                            if (total == 0) return 100.0; // sin eventos = cumple
                            return (svc.exitosas.get() * 100.0) / total;
                        })
                .description("Eficacia del envío de alertas de stock mínimo (objetivo 100%)")
                .tag("rnf", "RNF-08")
                .register(registry);
    }

    public void registrarIntento() {
        alertasIntentadasTotal.increment();
        intentadas.incrementAndGet();
    }

    public void registrarExito() {
        alertasExitosasTotal.increment();
        exitosas.incrementAndGet();
    }

    public void registrarFallo() {
        alertasFallidasTotal.increment();
    }

    public long getIntentadas() { return intentadas.get(); }
    public long getExitosas()   { return exitosas.get(); }
    public double getEficacia() {
        if (intentadas.get() == 0) return 100.0;
        return (exitosas.get() * 100.0) / intentadas.get();
    }
}
