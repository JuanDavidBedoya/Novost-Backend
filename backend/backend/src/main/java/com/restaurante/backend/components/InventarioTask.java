package com.restaurante.backend.components;

import com.restaurante.backend.services.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioTask {

    private final InventarioService inventarioService;

    // Reiniciar consumo diario a las 00:01 todos los días
    @Scheduled(cron = "0 1 0 * * ?")
    public void reiniciarConsumoDiario() {
        log.info("Ejecutando tarea programada: Reiniciar consumo diario del inventario");
        try {
            inventarioService.reiniciarConsumoDiario();
            log.info("Consumo diario del inventario reiniciado exitosamente");
        } catch (Exception e) {
            log.error("Error al reiniciar consumo diario del inventario: {}", e.getMessage());
        }
    }
}
