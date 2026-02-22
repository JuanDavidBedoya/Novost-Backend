package com.restaurante.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByIdPasarela(String idPasarela);

    // Busca el registro de pago asociado a una instancia de Reserva
    Optional<Pago> findByReserva(Reserva reserva);
    
    // Opcional: buscar por el ID de la reserva directamente
    Optional<Pago> findByReserva_IdReserva(Long idReserva);
}
