package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

// Servicio de métricas para monitorear fallos en creación de pedidos (RNF-11)

@Service
public class PedidoFalloMetricaService {

    // Atributos: contadores para cada tipo de fallo (carrito vacío, mesa no seleccionada, error servidor)

    private final Counter contadorCarritoVacio;
    private final Counter contadorMesaNoSeleccionada;
    private final Counter contadorErrorServidor;

    // Lista thread-safe y formato de fecha para historial de fallos

    private final List<Map<String, String>> historialFallos = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor: inicializa contadores en el registry con tags de RNF-11 y tipo de error

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

    // Método registrarFallo: incrementa contador según tipo de error y registra evento en historial con timestamp

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

    // Métodos getter: retornan historial completo de fallos y cantidad total

    public List<Map<String, String>> getHistorial() { return historialFallos; }
    public int getTotalFallos()                      { return historialFallos.size(); }
}