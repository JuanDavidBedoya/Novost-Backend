package com.restaurante.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.EstadoReserva;

public interface EstadoReservaRepository extends JpaRepository<EstadoReserva, Long> {
    
    // Busca el estado por su nombre exacto (ej: "PENDIENTE", "PAGADA", "CANCELADA")
    Optional<EstadoReserva> findByNombre(String nombre);
}
