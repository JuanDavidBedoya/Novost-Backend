package com.restaurante.backend.repositories;

import com.restaurante.backend.entities.PagoPedido;
import com.restaurante.backend.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoPedidoRepository extends JpaRepository<PagoPedido, Long> {

    Optional<PagoPedido> findByPedido(Pedido pedido);

    List<PagoPedido> findByFechaPagoBetween(LocalDateTime start, LocalDateTime end);
}