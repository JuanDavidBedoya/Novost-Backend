package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PedidoFalloMetricaService {

    private final Counter contadorCarritoVacio;
    private final Counter contadorMesaNoSeleccionada;
    private final Counter contadorErrorServidor;

    private final List<Map<String, String>> historialFallos = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PedidoFalloMetricaService(MeterRegistry registry) {

        this.contadorCarritoVacio = Counter.builder("novost_pedido_fallo_total")
                .description("Fallos en la creación de pedidos registrados desde el frontend")
                .tag("rnf",        "RNF-11")
                .tag("tipo_error", "CARRITO_VACIO")
                .register(registry);

        this.contadorMesaNoSeleccionada = Counter.builder("novost_pedido_fallo_total")
                .description("Fallos en la creación de pedidos registrados desde el frontend")
                .tag("rnf",        "RNF-11")
                .tag("tipo_error", "MESA_NO_SELECCIONADA")
                .register(registry);

        this.contadorErrorServidor = Counter.builder("novost_pedido_fallo_total")
                .description("Fallos en la creación de pedidos registrados desde el frontend")
                .tag("rnf",        "RNF-11")
                .tag("tipo_error", "ERROR_SERVIDOR")
                .register(registry);
    }

    public void registrarFallo(String tipoError, String motivo) {
        switch (tipoError) {
            case "CARRITO_VACIO"        -> contadorCarritoVacio.increment();
            case "MESA_NO_SELECCIONADA" -> contadorMesaNoSeleccionada.increment();
            default                     -> contadorErrorServidor.increment();
        }

        historialFallos.add(Map.of(
                "tipoError", tipoError,
                "motivo",    motivo != null ? motivo : "sin detalle",
                "fechaHora", LocalDateTime.now().format(FMT)
        ));
    }

    public List<Map<String, String>> getHistorial() { return historialFallos; }
    public int getTotalFallos()                      { return historialFallos.size(); }
}