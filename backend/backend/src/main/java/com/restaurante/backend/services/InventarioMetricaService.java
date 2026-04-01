package com.restaurante.backend.services;

import com.restaurante.backend.entities.Inventario.TipoMedida;
import com.restaurante.backend.repositories.InventarioRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class InventarioMetricaService {

    private final InventarioRepository inventarioRepository;

    public InventarioMetricaService(MeterRegistry registry,
                                    InventarioRepository inventarioRepository) {
        this.inventarioRepository = inventarioRepository;

        // Gauge: se recalcula en cada scrape de Prometheus automáticamente
        Gauge.builder("novost_inventario_productos_por_unidad",
                        inventarioRepository,
                        repo -> repo.countByTipoMedida(TipoMedida.KILO))
                .description("Cantidad de productos activos con unidad KG")
                .tag("rnf", "RNF-04")
                .tag("proceso", "gestion_inventario")
                .tag("unidad", "KG")
                .register(registry);

        Gauge.builder("novost_inventario_productos_por_unidad",
                        inventarioRepository,
                        repo -> repo.countByTipoMedida(TipoMedida.LITRO))
                .description("Cantidad de productos activos con unidad L")
                .tag("rnf", "RNF-04")
                .tag("proceso", "gestion_inventario")
                .tag("unidad", "L")
                .register(registry);

        Gauge.builder("novost_inventario_productos_por_unidad",
                        inventarioRepository,
                        repo -> repo.countByTipoMedida(TipoMedida.UNIDAD))
                .description("Cantidad de productos activos con unidad UND")
                .tag("rnf", "RNF-04")
                .tag("proceso", "gestion_inventario")
                .tag("unidad", "UND")
                .register(registry);
    }

    // Métodos de consulta directa (para el endpoint de resumen)
    public long getProductosKg()  {
        return inventarioRepository.countByTipoMedida(TipoMedida.KILO);
    }
    public long getProductosL()   {
        return inventarioRepository.countByTipoMedida(TipoMedida.LITRO);
    }
    public long getProductosUnd() {
        return inventarioRepository.countByTipoMedida(TipoMedida.UNIDAD);
    }
}
