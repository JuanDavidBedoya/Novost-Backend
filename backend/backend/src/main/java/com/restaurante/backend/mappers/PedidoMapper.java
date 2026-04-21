package com.restaurante.backend.mappers;

import com.restaurante.backend.dtos.PedidoResponseDTO;
import com.restaurante.backend.entities.Pedido;
import org.springframework.stereotype.Component;

// Mapper para convertir entidad Pedido a DTO de respuesta

@Component
public class PedidoMapper {

    // Método toResponseDTO: transforma entidad Pedido en PedidoResponseDTO mapeando datos básicos, mesa, reserva, montos y estado

    public PedidoResponseDTO toResponseDTO(Pedido pedido) {
        if (pedido == null) return null;
        
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setIdPedido(pedido.getIdPedido());
        dto.setFechaPedido(pedido.getFechaPedido());
        dto.setHoraPedido(pedido.getHoraPedido());
        
        if (pedido.getMesa() != null) {
            dto.setIdMesa(pedido.getMesa().getIdMesa());
            dto.setNumeroMesa(pedido.getMesa().getNumeroMesa());
        }
        
        if (pedido.getReserva() != null) {
            dto.setIdReserva(pedido.getReserva().getIdReserva());
        }
        
        dto.setSubtotal(pedido.getSubtotal());
        dto.setTotal(pedido.getTotal());
        dto.setObservaciones(pedido.getObservaciones());
        
        if (pedido.getEstadoPedido() != null) {
            dto.setEstadoPedido(pedido.getEstadoPedido().getNombre());
        }
        
        return dto;
    }
}