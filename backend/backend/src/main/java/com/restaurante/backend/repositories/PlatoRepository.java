package com.restaurante.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Plato;

import java.util.List;

public interface PlatoRepository extends JpaRepository<Plato, Long> {
    List<Plato> findByCategoriaNombreCategoriaAndEstado(String nombreCategoria, boolean estado);

    List<Plato> findByCategoriaNombreCategoria(String nombreCategoria);
}
