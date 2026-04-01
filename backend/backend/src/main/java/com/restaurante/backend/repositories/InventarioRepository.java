package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Inventario;
import com.restaurante.backend.entities.Inventario.TipoMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    long countByTipoMedida(TipoMedida tipoMedida);
    
    // Productos con stock por debajo del mínimo
    @Query("SELECT i FROM Inventario i WHERE i.stockActual < i.stockMinimo")
    List<Inventario> findProductosStockMinimo();

    // Productos más utilizados (por consumo hoy)
    @Query("SELECT i FROM Inventario i ORDER BY i.consumoHoy DESC")
    List<Inventario> findProductosMasUtilizados();

    // Productos por tipo de medida
    @Query("SELECT i FROM Inventario i WHERE i.tipoMedida = :tipoMedida")
    List<Inventario> findByTipoMedida(@Param("tipoMedida") TipoMedida tipoMedida);

    // Buscar alimento por nombre
    List<Inventario> findByNombreAlimentoContainingIgnoreCase(String nombreAlimento);

    // Contar productos por debajo del mínimo
    @Query("SELECT COUNT(i) FROM Inventario i WHERE i.stockActual < i.stockMinimo")
    Long countProductosStockMinimo();

    // Sumar stock total por tipo de medida
    @Query("SELECT COALESCE(SUM(i.stockActual), 0) FROM Inventario i WHERE i.tipoMedida = :tipoMedida")
    Double sumStockByTipoMedida(@Param("tipoMedida") TipoMedida tipoMedida);

    // Contar total de productos
    @Query("SELECT COUNT(i) FROM Inventario i")
    Long countTotalProductos();

    // Contar unidades
    @Query("SELECT COUNT(i) FROM Inventario i WHERE i.tipoMedida = 'UNIDAD'")
    Long countUnidades();

    // Sumar consumo del día
    @Query("SELECT COALESCE(SUM(i.consumoHoy), 0) FROM Inventario i")
    Double sumConsumoHoy();
}
