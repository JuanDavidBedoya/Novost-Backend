package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConversionReservaMetricaService {

    private final Counter reservasIntentadasTotal;
    private final Counter reservasPagadasTotal;
    private final Counter reservasAbandonadasTotal;

    // Para el Gauge de eficacia
    private final AtomicLong intentadas = new AtomicLong(0);
    private final AtomicLong pagadas    = new AtomicLong(0);

    public ConversionReservaMetricaService(MeterRegistry registry) {

        this.reservasIntentadasTotal = Counter.builder("novost_reserva_conversion_intentadas_total")
                .description("Total de reservas creadas (intención de reserva iniciada)")
                .tag("rnf", "RNF-10")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        this.reservasPagadasTotal = Counter.builder("novost_reserva_conversion_pagadas_total")
                .description("Total de reservas que completaron el pago exitosamente")
                .tag("rnf", "RNF-10")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        this.reservasAbandonadasTotal = Counter.builder("novost_reserva_conversion_abandonadas_total")
                .description("Total de reservas canceladas sin haber sido pagadas")
                .tag("rnf", "RNF-10")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        // Gauge: eficacia = pagadas / intentadas * 100 (objetivo > 85%)
        Gauge.builder("novost_reserva_conversion_eficacia_porcentaje",
                        this,
                        svc -> {
                            long total = svc.intentadas.get();
                            if (total == 0) return 0.0;
                            return (svc.pagadas.get() * 100.0) / total;
                        })
                .description("Eficacia de conversión de reservas a pagos (objetivo > 85%)")
                .tag("rnf", "RNF-10")
                .register(registry);
    }

    public void registrarIntento() {
        reservasIntentadasTotal.increment();
        intentadas.incrementAndGet();
    }

    public void registrarPago() {
        reservasPagadasTotal.increment();
        pagadas.incrementAndGet();
    }

    public void registrarAbandonada() {
        reservasAbandonadasTotal.increment();
    }

    public long   getIntentadas()  { return intentadas.get(); }
    public long   getPagadas()     { return pagadas.get(); }
    public double getEficacia() {
        if (intentadas.get() == 0) return 0.0;
        return (pagadas.get() * 100.0) / intentadas.get();
    }
}
