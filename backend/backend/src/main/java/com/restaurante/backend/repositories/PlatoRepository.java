package com.restaurante.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Plato;

import java.util.List;

public interface PlatoRepository extends JpaRepository<Plato, Long> {

    // Método findByCategoriaNombreCategoriaAndEstado: obtiene platos de una categoría específica según estado

    List<Plato> findByCategoriaNombreCategoriaAndEstado(String nombreCategoria, boolean estado);
    
    // Método findByCategoriaNombreCategoria: obtiene todos los platos de una categoría

    List<Plato> findByCategoriaNombreCategoria(String nombreCategoria);
}
