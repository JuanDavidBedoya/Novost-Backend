package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Pedido;
import com.restaurante.backend.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Pedidos del usuario logueado con filtros opcionales de fecha y estado.
     * Usado en: GET /pedidos/mis-pedidos
     */
    @Query("""
            SELECT p FROM Pedido p
            WHERE p.usuario = :usuario
              AND (:fecha  IS NULL OR p.fechaPedido         = :fecha)
              AND (:estado IS NULL OR p.estadoPedido.nombre = :estado)
            ORDER BY p.fechaPedido DESC, p.horaPedido DESC
            """)
    List<Pedido> findByUsuarioWithFilters(
            @Param("usuario") Usuario usuario,
            @Param("fecha")   LocalDate fecha,
            @Param("estado")  String estado
    );

    /**
     * Todos los pedidos (vista trabajador) con filtros opcionales de fecha y estado.
     * Usado en: GET /pedidos/todos
     */
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

    // ── Queries para Dashboard de Clientes ───────────────────────────────────

    @Query("SELECT COUNT(p) FROM Pedido p " +
           "WHERE p.fechaPedido = :fecha " +
           "AND p.estadoPedido.nombre IN ('PAGADO', 'ENTREGADO')")
    Long countByFechaPedido(@Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(p) FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin " +
           "AND p.estadoPedido.nombre IN ('PAGADO', 'ENTREGADO')")
    Long countByFechaPedidoBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);

    @Query("SELECT DAYOFWEEK(p.fechaPedido), COUNT(p) " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin " +
           "AND p.estadoPedido.nombre IN ('PAGADO', 'ENTREGADO') " +
           "GROUP BY DAYOFWEEK(p.fechaPedido) " +
           "ORDER BY DAYOFWEEK(p.fechaPedido)")
    List<Object[]> countAgrupadoPorDiaSemana(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);
}