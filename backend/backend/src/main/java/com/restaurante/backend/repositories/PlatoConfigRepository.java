package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Plato;
import com.restaurante.backend.entities.PlatoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatoConfigRepository extends JpaRepository<PlatoConfig, Long> {
    Optional<PlatoConfig> findByPlato(Plato plato);
}