package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Pedido;
import com.restaurante.backend.entities.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

  // Método findByPedido: obtiene todos los detalles asociados a un pedido

    List<PedidoDetalle> findByPedido(Pedido pedido);

    // Query ventasPorPlatoEnFecha: agrupa ventas por plato en una fecha (solo pedidos pagados/entregados) ordenados por cantidad

    @Query("""
            SELECT
                pd.plato.nombrePlato,
                SUM(pd.cantidad),
                SUM(pd.subtotal)
            FROM PedidoDetalle pd
            WHERE pd.pedido.fechaPedido = :fecha
              AND pd.pedido.estadoPedido.nombre IN ('PAGADO', 'ENTREGADO')
            GROUP BY pd.plato.nombrePlato
            ORDER BY SUM(pd.cantidad) DESC
            """)
    List<Object[]> ventasPorPlatoEnFecha(@Param("fecha") LocalDate fecha);

    // Query ventasPorCategoriaEnFecha: agrupa ventas por categoría en una fecha (solo pedidos pagados/entregados) ordenados por cantidad

    @Query("""
            SELECT
                pd.plato.categoria.nombreCategoria,
                SUM(pd.cantidad),
                SUM(pd.subtotal)
            FROM PedidoDetalle pd
            WHERE pd.pedido.fechaPedido = :fecha
              AND pd.pedido.estadoPedido.nombre IN ('PAGADO', 'ENTREGADO')
            GROUP BY pd.plato.categoria.nombreCategoria
            ORDER BY SUM(pd.cantidad) DESC
            """)
    List<Object[]> ventasPorCategoriaEnFecha(@Param("fecha") LocalDate fecha);

    /**
     * Agrupa las unidades vendidas por hora del dia para una fecha dada.
     * Solo incluye pedidos en estado PAGADO o ENTREGADO.
     * Retorna: [hora (int), unidadesTotal]
     */
    @Query("SELECT FUNCTION('HOUR', pd.pedido.horaPedido), SUM(pd.cantidad) " +
           "FROM PedidoDetalle pd " +
           "WHERE pd.pedido.fechaPedido = :fecha " +
           "AND pd.pedido.estadoPedido.nombre IN ('PAGADO', 'ENTREGADO') " +
           "GROUP BY FUNCTION('HOUR', pd.pedido.horaPedido) " +
           "ORDER BY FUNCTION('HOUR', pd.pedido.horaPedido)")
    List<Object[]> unidadesPorHoraEnFecha(@Param("fecha") LocalDate fecha);
}