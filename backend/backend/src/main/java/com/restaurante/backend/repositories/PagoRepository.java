package com.restaurante.backend.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Método findByIdPasarela: obtiene pago por ID de pasarela (Stripe)

    Optional<Pago> findByIdPasarela(String idPasarela);

    // Método findByReserva: obtiene pago asociado a una reserva

    Optional<Pago> findByReserva(Reserva reserva);

    // Método findByReserva_IdReserva: obtiene pago por ID de reserva (proyección)
    
    Optional<Pago> findByReserva_IdReserva(Long idReserva);

    // Método findByFechaPagoBetween: obtiene pagos realizados en un rango de fechas

    List<Pago> findByFechaPagoBetween(LocalDateTime start, LocalDateTime end);
}
