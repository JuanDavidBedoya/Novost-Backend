package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TipoProductoRepository extends JpaRepository<TipoProducto, Long> {
    List<TipoProducto> findByActivoTrue();
    Optional<TipoProducto> findByNombreTipo(String nombreTipo);
    boolean existsByNombreTipo(String nombreTipo);
}