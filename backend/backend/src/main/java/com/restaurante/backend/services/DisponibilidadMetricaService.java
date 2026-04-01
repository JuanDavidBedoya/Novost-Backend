package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DisponibilidadMetricaService {

    private final Counter consultasTotal;
    private final Counter errores500Total;

    // Momento del último error 500 (epoch seconds). -1 = nunca ha fallado
    private final AtomicReference<Instant> ultimoError = new AtomicReference<>(null);

    // MTBF en segundos (se recalcula cada vez que ocurre un nuevo fallo)
    private final AtomicLong mtbfSegundos = new AtomicLong(0);

    // Momento en que se inició el servicio
    private final Instant inicio = Instant.now();

    public DisponibilidadMetricaService(MeterRegistry registry) {

        this.consultasTotal = Counter.builder("novost_disponibilidad_consultas_total")
                .description("Total de consultas al buscador de mesas disponibles")
                .tag("rnf", "RNF-06")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        this.errores500Total = Counter.builder("novost_disponibilidad_errores500_total")
                .description("Total de errores 500 en el buscador de mesas")
                .tag("rnf", "RNF-06")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        // Gauge: MTBF en horas (lo que necesita el criterio de aceptación)
        Gauge.builder("novost_disponibilidad_mtbf_horas",
                        mtbfSegundos,
                        val -> val.get() / 3600.0)
                .description("MTBF del buscador de mesas en horas (objetivo > 24h)")
                .tag("rnf", "RNF-06")
                .tag("proceso", "reserva_mesas")
                .register(registry);

        // Gauge: segundos desde el último error (o desde inicio si nunca ha fallado)
        Gauge.builder("novost_disponibilidad_segundos_desde_ultimo_error",
                        this,
                        svc -> {
                            Instant ultimo = svc.ultimoError.get();
                            Instant referencia = ultimo != null ? ultimo : svc.inicio;
                            return (double) (Instant.now().getEpochSecond()
                                    - referencia.getEpochSecond());
                        })
                .description("Segundos transcurridos desde el último error 500")
                .tag("rnf", "RNF-06")
                .register(registry);
    }

    public void registrarConsulta() {
        consultasTotal.increment();
    }

    public void registrarError500() {
        Instant ahora = Instant.now();
        Instant anterior = ultimoError.getAndSet(ahora);

        errores500Total.increment();

        // Calcular MTBF: tiempo entre el error anterior y este
        // Si es el primer error, usamos el tiempo desde el inicio del servicio
        Instant referencia = anterior != null ? anterior : inicio;
        long segundosEntreErrores = ahora.getEpochSecond() - referencia.getEpochSecond();
        mtbfSegundos.set(segundosEntreErrores);
    }

    public double getConsultas()      { return consultasTotal.count(); }
    public double getErrores500()     { return errores500Total.count(); }
    public long   getMtbfSegundos()   { return mtbfSegundos.get(); }
    public double getMtbfHoras()      { return mtbfSegundos.get() / 3600.0; }
    public Instant getUltimoError()   { return ultimoError.get(); }
    public Instant getInicio()        { return inicio; }
}