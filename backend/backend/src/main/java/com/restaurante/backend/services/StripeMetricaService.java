package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class StripeMetricaService {

    private final Counter intentosTotal;
    private final Counter exitosTotal;
    private final Counter fallosTecnicosTotal;

    public StripeMetricaService(MeterRegistry registry) {

        this.intentosTotal = Counter.builder("novost_stripe_intentos_total")
                .description("Total de intentos de comunicación con Stripe")
                .tag("rnf", "RNF-02")
                .tag("proceso", "reserva_pago")
                .register(registry);

        this.exitosTotal = Counter.builder("novost_stripe_exitos_total")
                .description("Comunicaciones exitosas con Stripe")
                .tag("rnf", "RNF-02")
                .tag("proceso", "reserva_pago")
                .register(registry);

        this.fallosTecnicosTotal = Counter.builder("novost_stripe_fallos_tecnicos_total")
                .description("Fallos técnicos en la comunicación con Stripe (StripeException)")
                .tag("rnf", "RNF-02")
                .tag("proceso", "reserva_pago")
                .register(registry);
    }

    public void registrarIntento()       { intentosTotal.increment(); }
    public void registrarExito()         { exitosTotal.increment(); }
    public void registrarFalloTecnico()  { fallosTecnicosTotal.increment(); }

    public double getIntentos()      { return intentosTotal.count(); }
    public double getExitos()        { return exitosTotal.count(); }
    public double getFallosTecnicos(){ return fallosTecnicosTotal.count(); }
}