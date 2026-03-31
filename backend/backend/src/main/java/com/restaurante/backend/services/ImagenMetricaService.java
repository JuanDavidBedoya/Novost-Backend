package com.restaurante.backend.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ImagenMetricaService {

    private final Counter imagenesExitosas;
    private final Counter imagenesFallidas;

    public ImagenMetricaService(MeterRegistry registry) {
        // Contador de imágenes que cargaron correctamente
        this.imagenesExitosas = Counter.builder("cloudinary_imagenes_exitosas_total")
                .description("Total de imágenes de Cloudinary cargadas correctamente")
                .tag("rnf", "RNF-03")
                .register(registry);

        // Contador de imágenes que fallaron al cargar
        this.imagenesFallidas = Counter.builder("cloudinary_imagenes_fallidas_total")
                .description("Total de imágenes de Cloudinary que fallaron al cargar")
                .tag("rnf", "RNF-03")
                .register(registry);
    }

    public void registrarExito() {
        imagenesExitosas.increment();
    }

    public void registrarFallo() {
        imagenesFallidas.increment();
    }

    public double getTotalExitosas() {
        return imagenesExitosas.count();
    }

    public double getTotalFallidas() {
        return imagenesFallidas.count();
    }
}