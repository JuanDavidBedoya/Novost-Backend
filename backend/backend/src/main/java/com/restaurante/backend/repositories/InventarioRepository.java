package com.restaurante.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
}
