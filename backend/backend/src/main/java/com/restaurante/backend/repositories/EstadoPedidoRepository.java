package com.restaurante.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.EstadoPedido;

public interface EstadoPedidoRepository extends JpaRepository<EstadoPedido, Long> {

        //Encontrar por nombre
    
        Optional<EstadoPedido> findByNombre(String nombre);

}
