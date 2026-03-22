package com.restaurante.backend.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.restaurante.backend.entities.Reserva;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) 
    @Query("SELECT r FROM Reserva r WHERE r.mesa.idMesa = :idMesa " +
        "AND r.fecha = :fecha " +
        "AND (r.estadoReserva.nombre <> 'CANCELADA') " +
        "AND NOT (r.horaFin <= :inicio OR r.horaInicio >= :fin)")
    List<Reserva> findOverlappingReservations(
        @Param("idMesa") Long idMesa, 
        @Param("fecha") LocalDate fecha, 
        @Param("inicio") LocalTime inicio, 
        @Param("fin") LocalTime fin
    );

    @Query("SELECT r FROM Reserva r WHERE r.estadoReserva.nombre = 'PENDIENTE' " +
       "AND (" +
       "  r.fecha < :fechaReferencia " + 
       "  OR (r.fecha = :fechaReferencia AND r.horaInicio <= :horaReferencia)" +
       ") " +
       "AND NOT EXISTS (SELECT p FROM Pago p WHERE p.reserva = r AND p.estadoPago = 'PAGADO')")
    List<Reserva> findReservasVencidas(
        @Param("fechaReferencia") LocalDate fechaReferencia, 
        @Param("horaReferencia") LocalTime horaReferencia
    );

    @Query("SELECT r FROM Reserva r WHERE r.fecha = :fecha " +
       "AND r.horaInicio >= :horaInicio " +
       "AND r.horaInicio < :horaFin " +
       "AND r.estadoReserva.nombre = 'PAGADA'")
    List<Reserva> findReservasParaRecordatorio(
        @Param("fecha") LocalDate fecha, 
        @Param("horaInicio") LocalTime horaInicio, 
        @Param("horaFin") LocalTime horaFin
    );

    @Query("SELECT r FROM Reserva r WHERE r.estadoReserva.nombre <> 'CANCELADA' AND " +
           "(:fecha IS NULL OR r.fecha = :fecha) AND " +
           "(:hora IS NULL OR r.horaInicio = :hora) AND " +
           "(:personas IS NULL OR r.numPersonas = :personas)")
    List<Reserva> buscarConFiltros(
        @Param("fecha") LocalDate fecha, 
        @Param("hora") LocalTime hora, 
        @Param("personas") Integer personas
    );

    @Query("SELECT r FROM Reserva r WHERE " +
           "(:fecha IS NULL OR r.fecha = :fecha) AND " +
           "(:hora IS NULL OR r.horaInicio = :hora) AND " +
           "(:personas IS NULL OR r.numPersonas = :personas)")
    List<Reserva> buscarTodasConFiltros(
        @Param("fecha") LocalDate fecha, 
        @Param("hora") LocalTime hora, 
        @Param("personas") Integer personas
    );

    @Query("SELECT r FROM Reserva r WHERE r.estadoReserva.nombre <> 'CANCELADA' AND r.fecha = :fecha")
    List<Reserva> buscarReservasPorFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT r FROM Reserva r WHERE r.usuario.cedula = :cedula")
    List<Reserva> findByUsuarioCedula(@Param("cedula") String cedula);

    @Query("SELECT r FROM Reserva r WHERE r.usuario.cedula = :cedula AND " +
           "(:fecha IS NULL OR r.fecha = :fecha) AND " +
           "(:hora IS NULL OR r.horaInicio = :hora) AND " +
           "(:personas IS NULL OR r.numPersonas = :personas)")
    List<Reserva> buscarPorUsuarioConFiltros(
        @Param("cedula") String cedula,
        @Param("fecha") LocalDate fecha, 
        @Param("hora") LocalTime hora, 
        @Param("personas") Integer personas
    );

    @Query("SELECT r FROM Reserva r WHERE r.usuario.cedula = :cedula AND " +
           "(:fecha IS NULL OR r.fecha = :fecha) AND " +
           "(:hora IS NULL OR r.horaInicio = :hora) AND " +
           "(:personas IS NULL OR r.numPersonas = :personas)")
    List<Reserva> buscarTodasPorUsuarioConFiltros(
        @Param("cedula") String cedula,
        @Param("fecha") LocalDate fecha, 
        @Param("hora") LocalTime hora, 
        @Param("personas") Integer personas
    );

    @Query("SELECT r FROM Reserva r WHERE r.mesa.idMesa = :idMesa " +
           "AND r.fecha = :fecha " +
           "AND r.horaInicio <= :horaActual " +
           "AND r.horaFin >= :horaActual " +
           "AND r.estadoReserva.nombre <> 'CANCELADA'")
    Optional<Reserva> findReservaActivaPorMesaYHora(
        @Param("idMesa") Long idMesa, 
        @Param("fecha") LocalDate fecha, 
        @Param("horaActual") LocalTime horaActual
    );
}