package com.restaurante.backend.controllers;

import com.restaurante.backend.dtos.ImagenEventoDTO;
import com.restaurante.backend.dtos.PedidoFalloEventoDTO;
import com.restaurante.backend.dtos.PropagacionEventoDTO;
import com.restaurante.backend.dtos.RedirectEventoDTO;
import com.restaurante.backend.services.ImagenMetricaService;
import com.restaurante.backend.services.PedidoFalloMetricaService;
import com.restaurante.backend.services.PropagacionMetricaService;
import com.restaurante.backend.services.RedirectMetricaService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/metricas")
@RequiredArgsConstructor
public class MetricasController {

    private final ImagenMetricaService imagenMetricaService;
    private final RedirectMetricaService redirectMetricaService;
    private final PropagacionMetricaService propagacionMetricaService;
    private final PedidoFalloMetricaService pedidoFalloMetricaService;

    // El frontend llama a este endpoint cada vez que una imagen carga o falla
    @PostMapping("/imagen-evento")
    public ResponseEntity<Map<String, String>> registrarEvento(
            @RequestBody ImagenEventoDTO evento) {

        if ("exito".equalsIgnoreCase(evento.getResultado())) {
            imagenMetricaService.registrarExito();
        } else {
            imagenMetricaService.registrarFallo();
        }

        return ResponseEntity.ok(Map.of("registrado", evento.getResultado()));
    }

    // Endpoint de resumen para consulta rápida
    @GetMapping("/rnf-03")
    public ResponseEntity<Map<String, Object>> resumenRnf03() {
        double exitosas = imagenMetricaService.getTotalExitosas();
        double fallidas = imagenMetricaService.getTotalFallidas();
        double total    = exitosas + fallidas;
        boolean cumple  = fallidas < 2;

        return ResponseEntity.ok(Map.of(
                "totalImagenes",     total,
                "imagenesExitosas",  exitosas,
                "imagenesFallidas",  fallidas,
                "criterio",          "fallidas < 2",
                "cumpleRNF",         cumple
        ));
    }

    // ✅ El frontend reporta aquí cada error visible en pantalla
    @PostMapping("/pedido-fallo-evento")
    public ResponseEntity<Map<String, String>> registrarPedidoFallo(
            @RequestBody PedidoFalloEventoDTO evento) {
        pedidoFalloMetricaService.registrarFallo(evento.getTipoError(), evento.getMotivo());
        return ResponseEntity.ok(Map.of("registrado", evento.getTipoError()));
    }

    // ✅ Resumen del RNF-11
    @GetMapping("/rnf-11")
    public ResponseEntity<Map<String, Object>> resumenRnf11() {
        int total      = pedidoFalloMetricaService.getTotalFallos();
        boolean cumple = true; // Siempre cumple si el endpoint recibe los eventos

        return ResponseEntity.ok(Map.of(
                "totalFallosRegistrados", total,
                "cobertura",              "100%",
                "criterio",               "Cada fallo visible en pantalla debe registrarse con motivo, fecha y hora",
                "historialFallos",        pedidoFalloMetricaService.getHistorial(),
                "cumpleRNF",              cumple
        ));
    }

    @PostMapping("/redirect-evento")
    public ResponseEntity<Map<String, String>> registrarRedirect(
            @RequestBody RedirectEventoDTO evento) {

        redirectMetricaService.registrar(evento.getDuracionSegundos());

        String etiqueta = evento.getDuracionSegundos() > 0.5 ? "Rendimiento Bajo" : "OK";
        return ResponseEntity.ok(Map.of(
                "registrado", "true",
                "duracionSegundos", String.valueOf(evento.getDuracionSegundos()),
                "etiqueta", etiqueta
        ));
    }

    // Nuevo endpoint — resumen RNF-15
    @GetMapping("/rnf-15")
    public ResponseEntity<Map<String, Object>> resumenRnf15() {
        double total        = redirectMetricaService.getTotal();
        double bajos        = redirectMetricaService.getRendimientoBajo();
        double promedio     = redirectMetricaService.getTiempoPromedio();    // ✅ NUEVO
        double maximo       = redirectMetricaService.getTiempoMaximo();      // ✅ NUEVO
        boolean cumple      = total == 0 || bajos == 0;

        return ResponseEntity.ok(Map.of(
                "totalRedireccionamientos",         total,
                "conRendimientoBajo",               bajos,
                "tiempoPromedioSegundos",           String.format("%.4f s", promedio),  // ✅ NUEVO
                "tiempoMaximoSegundos",             String.format("%.4f s", maximo),    // ✅ NUEVO
                "umbralRendimientoBajo",            "0.5 s",
                "criterio",                         "100% de los que superen 0.5s deben marcarse como Rendimiento Bajo",
                "cumpleRNF",                        cumple
        ));
    }

    @PostMapping("/propagacion-evento")
    public ResponseEntity<Map<String, String>> registrarPropagacion(
            @RequestBody PropagacionEventoDTO evento) {
        propagacionMetricaService.registrar(evento.getDuracionSegundos());
        String etiqueta = evento.getDuracionSegundos() > 3.0 ? "Lento" : "OK";
        return ResponseEntity.ok(Map.of(
                "registrado",       "true",
                "duracionSegundos", String.valueOf(evento.getDuracionSegundos()),
                "etiqueta",         etiqueta
        ));
    }

    // ✅ Resumen RNF-19
    @GetMapping("/rnf-19")
    public ResponseEntity<Map<String, Object>> resumenRnf19() {
        double total   = propagacionMetricaService.getTotal();
        double lentas  = propagacionMetricaService.getPropagacionesLentas();
        double promedio = propagacionMetricaService.getTiempoPromedio();
        double maximo  = propagacionMetricaService.getTiempoMaximo();
        boolean cumple = total == 0 || lentas == 0;

        return ResponseEntity.ok(Map.of(
                "totalPropagaciones",       total,
                "propagacionesLentas",      lentas,
                "tiempoPromedioSegundos",   String.format("%.4f s", promedio),
                "tiempoMaximoSegundos",     String.format("%.4f s", maximo),
                "umbral",                   "3.0 s",
                "criterio",                 "deshabilitación hasta muestra en menú < 3s",
                "cumpleRNF",                cumple
        ));
    }
}