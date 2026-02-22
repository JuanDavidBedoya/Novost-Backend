package com.restaurante.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Mesa;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    
    List<Mesa> findByCapacidadGreaterThanEqualOrderByCapacidadAsc(Integer capacidad);
}
