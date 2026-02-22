package com.restaurante.backend.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de response para devuelve datos de una reserva
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaResponseDTO {

    private Long idReserva;
    private String cedulaUsuario;
    private String nombreUsuario;
    private String emailUsuario;
    private Long idMesa;
    private Integer numeroMesa;
    private Integer capacidadMesa;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer numPersonas;
    private String estadoReserva;
}
