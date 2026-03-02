package com.restaurante.backend.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaRequestDTO {

    private String cedulaUsuario;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private Integer numPersonas;
}
