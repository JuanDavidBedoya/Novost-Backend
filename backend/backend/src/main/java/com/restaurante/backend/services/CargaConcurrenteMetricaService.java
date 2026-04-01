package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CargaConcurrenteMetricaService {

    private final Counter reservasExitosasTotal;
    private final Counter reservasFallidasTotal;
    private final Counter reservasIntentadasTotal;

    // Reservas actualmente en procesamiento (concurrencia en tiempo real)
    private final AtomicInteger reservasEnProceso = new AtomicInteger(0);

    // Para calcular tasa de éxito
    private final AtomicLong intentadas = new AtomicLong(0);
    private final AtomicLong exitosas   = new AtomicLong(0);

    // Pico máximo de concurrencia registrado
    private final AtomicInteger picoConcurrencia = new AtomicInteger(0);

    public CargaConcurrenteMetricaService(MeterRegistry registry) {

        this.reservasIntentadasTotal = Counter.builder("novost_carga_reservas_intentadas_total")
                .description("Total de reservas iniciadas (en procesamiento)")
                .tag("rnf", "RNF-18")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        this.reservasExitosasTotal = Counter.builder("novost_carga_reservas_exitosas_total")
                .description("Total de reservas completadas exitosamente")
                .tag("rnf", "RNF-18")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        this.reservasFallidasTotal = Counter.builder("novost_carga_reservas_fallidas_total")
                .description("Total de reservas que fallaron bajo carga")
                .tag("rnf", "RNF-18")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        // Gauge: reservas actualmente en procesamiento (concurrencia instantánea)
        Gauge.builder("novost_carga_reservas_en_proceso",
                        reservasEnProceso,
                        AtomicInteger::get)
                .description("Reservas siendo procesadas simultáneamente en este momento")
                .tag("rnf", "RNF-18")
                .register(registry);

        // Gauge: pico máximo de concurrencia registrado
        Gauge.builder("novost_carga_pico_concurrencia",
                        picoConcurrencia,
                        AtomicInteger::get)
                .description("Máximo de reservas simultáneas procesadas en cualquier momento")
                .tag("rnf", "RNF-18")
                .register(registry);

        // Gauge: tasa de éxito bajo carga (objetivo: estable, no debe degradarse)
        Gauge.builder("novost_carga_tasa_exito_porcentaje",
                        this,
                        svc -> {
                            long total = svc.intentadas.get();
                            if (total == 0) return 100.0;
                            return (svc.exitosas.get() * 100.0) / total;
                        })
                .description("Tasa de reservas exitosas sobre intentadas (debe mantenerse estable bajo carga)")
                .tag("rnf", "RNF-18")
                .register(registry);
    }

    /** Llamar al INICIO del procesamiento de crearReserva */
    public void registrarInicio() {
        reservasIntentadasTotal.increment();
        intentadas.incrementAndGet();

        // Incrementar concurrencia y actualizar pico si corresponde
        int actual = reservasEnProceso.incrementAndGet();
        picoConcurrencia.updateAndGet(pico -> Math.max(pico, actual));
    }

    /** Llamar cuando crearReserva termina EXITOSAMENTE */
    public void registrarExito() {
        reservasExitosasTotal.increment();
        exitosas.incrementAndGet();
        reservasEnProceso.decrementAndGet();
    }

    /** Llamar cuando crearReserva termina con ERROR */
    public void registrarFallo() {
        reservasFallidasTotal.increment();
        reservasEnProceso.decrementAndGet();
    }

    public long   getIntentadas()       { return intentadas.get(); }
    public long   getExitosas()         { return exitosas.get(); }
    public int    getConcurrenciaActual(){ return reservasEnProceso.get(); }
    public int    getPicoConcurrencia() { return picoConcurrencia.get(); }
    public double getTasaExito() {
        long total = intentadas.get();
        if (total == 0) return 100.0;
        return (exitosas.get() * 100.0) / total;
    }
}
