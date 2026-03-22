package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Pedido;
import com.restaurante.backend.entities.Usuario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("""
            SELECT p FROM Pedido p
            WHERE p.usuario = :usuario
              AND (:fecha  IS NULL OR p.fechaPedido  = :fecha)
              AND (:estado IS NULL OR p.estadoPedido.nombre = :estado)
            ORDER BY p.fechaPedido DESC, p.horaPedido DESC
            """)
    List<Pedido> findByUsuarioWithFilters(
            @Param("usuario") Usuario usuario,
            @Param("fecha")   LocalDate fecha,
            @Param("estado")  String estado
    );

    @Query("""
            SELECT p FROM Pedido p
            WHERE (:fecha  IS NULL OR p.fechaPedido         = :fecha)
              AND (:estado IS NULL OR p.estadoPedido.nombre = :estado)
            ORDER BY p.fechaPedido DESC, p.horaPedido DESC
            """)
    List<Pedido> findAllWithFilters(
            @Param("fecha")  LocalDate fecha,
            @Param("estado") String estado
    );

    
}