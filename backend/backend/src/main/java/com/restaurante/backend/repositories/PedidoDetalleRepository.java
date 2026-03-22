package com.restaurante.backend.repositories;


import com.restaurante.backend.entities.Pedido;
import com.restaurante.backend.entities.PedidoDetalle;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

    List<PedidoDetalle> findByPedido(Pedido pedido);
}