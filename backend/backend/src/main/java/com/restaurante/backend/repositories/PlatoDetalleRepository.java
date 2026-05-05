package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.Plato;
import com.restaurante.backend.entities.PlatoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Set;

public interface PlatoDetalleRepository extends JpaRepository<PlatoDetalle, Long> {

    // Método findByPlato: obtiene todos los ingredientes de un plato

    List<PlatoDetalle> findByPlato(Plato plato);

    // Método findByInventarioIdAlimentoIn: obtiene detalles de platos que usan ciertos alimentos
    
    List<PlatoDetalle> findByInventarioIdAlimentoIn(Set<Long> idsAlimento); 
}