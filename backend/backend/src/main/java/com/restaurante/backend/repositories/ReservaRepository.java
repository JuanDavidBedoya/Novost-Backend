package com.restaurante.backend.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurante.backend.entities.Reserva;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    // Busca si ya existe una reserva para esa mesa en ese rango de tiempo
    @Lock(LockModeType.PESSIMISTIC_WRITE) // Bloquea la fila durante la transacción
    @Query("SELECT r FROM Reserva r WHERE r.mesa.idMesa = :idMesa " +
           "AND r.fecha = :fecha " +
           "AND (:inicio < r.horaFin AND :fin > r.horaInicio)")
    List<Reserva> findOverlappingReservations(
        @Param("idMesa") Long idMesa, 
        @Param("fecha") LocalDate fecha, 
        @Param("inicio") LocalTime inicio, 
        @Param("fin") LocalTime fin
    );

    // Busca reservas donde:
    // 1. La fecha sea hoy o mañana (faltan menos de 24h)
    // 2. El estado sea 'PENDIENTE'
    // 3. No exista un registro de Pago exitoso asociado
    @Query("SELECT r FROM Reserva r WHERE r.fecha <= :fechaLimite " +
           "AND r.estadoReserva.nombre = 'PENDIENTE' " +
           "AND NOT EXISTS (SELECT p FROM Pago p WHERE p.reserva = r AND p.idEstadoPago = 'PAGADO')")
    List<Reserva> findReservasNoPagadasVencidas(@Param("fechaLimite") LocalDate fechaLimite);

    @Query("SELECT r FROM Reserva r WHERE r.fecha = :fecha " +
       "AND r.horaInicio >= :horaInicio " +
       "AND r.horaInicio < :horaFin " +
       "AND r.estadoReserva.nombre = 'PAGADA'")
    List<Reserva> findReservasParaRecordatorio(
        @Param("fecha") LocalDate fecha, 
        @Param("horaInicio") LocalTime horaInicio, 
        @Param("horaFin") LocalTime horaFin
    );

    // Buscar por filtro de fecha, hora y número de personas (todos opcionales)
    @Query("SELECT r FROM Reserva r WHERE " +
           "(:fecha IS NULL OR r.fecha = :fecha) AND " +
           "(:hora IS NULL OR r.horaInicio = :hora) AND " +
           "(:personas IS NULL OR r.numPersonas = :personas)")
    List<Reserva> buscarConFiltros(
        @Param("fecha") LocalDate fecha, 
        @Param("hora") LocalTime hora, 
        @Param("personas") Integer personas
    );
}