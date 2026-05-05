package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.ProductoIndividual;
import com.restaurante.backend.entities.ProductoIndividual.EstadoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoIndividualRepository extends JpaRepository<ProductoIndividual, Long> {
    
    List<ProductoIndividual> findByInventarioIdAlimento(Long idAlimento);
    
    @Query("SELECT p FROM ProductoIndividual p JOIN FETCH p.inventario WHERE p.inventario.idAlimento = :idAlimento AND p.estado = 'DISPONIBLE' ORDER BY p.fechaVencimiento ASC")
    List<ProductoIndividual> findProductosDisponiblesByAlimento(@Param("idAlimento") Long idAlimento);
    
    @Query("SELECT p FROM ProductoIndividual p JOIN FETCH p.inventario WHERE p.fechaVencimiento <= :fechaLimite AND p.estado = 'DISPONIBLE'")
    List<ProductoIndividual> findProductosProximosAVencer(@Param("fechaLimite") LocalDate fechaLimite);
    
    @Query("SELECT p FROM ProductoIndividual p JOIN FETCH p.inventario WHERE p.fechaVencimiento < :hoy AND p.estado = 'DISPONIBLE'")
    List<ProductoIndividual> findProductosVencidos(@Param("hoy") LocalDate hoy);
    
    @Query("SELECT p FROM ProductoIndividual p JOIN FETCH p.inventario WHERE p.inventario.tipoProducto.idTipo = :idTipo ORDER BY p.fechaVencimiento ASC")
    List<ProductoIndividual> findByTipoProducto(@Param("idTipo") Long idTipo);
    
    Long countByEstado(EstadoProducto estado);
    
    Long countByInventarioIdAlimentoAndEstado(Long idAlimento, EstadoProducto estado);
    
    @Query("SELECT p FROM ProductoIndividual p JOIN FETCH p.inventario WHERE p.idProducto = :idProducto")
    Optional<ProductoIndividual> findByIdWithInventario(@Param("idProducto") Long idProducto);
}