package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Plato;
import com.restaurante.backend.entities.PlatoImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatoImagenRepository extends JpaRepository<PlatoImagen, Long> {
    Optional<PlatoImagen> findByPlato(Plato plato);
}