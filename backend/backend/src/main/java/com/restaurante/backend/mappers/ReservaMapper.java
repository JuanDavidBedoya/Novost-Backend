package com.restaurante.backend.mappers;

import org.springframework.stereotype.Component;

import com.restaurante.backend.dtos.ReservaRequestDTO;
import com.restaurante.backend.dtos.ReservaResponseDTO;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

// Mapper para convertir entre DTO y entidad Reserva

@Component
@RequiredArgsConstructor
public class ReservaMapper {

    // Inyección de dependencia: repositorio de usuarios para obtener datos del usuario

    private final UsuarioRepository usuarioRepo;

    // Método toEntity: convierte ReservaRequestDTO a entidad Reserva, obtiene usuario y asigna datos básicos

    public Reserva toEntity(ReservaRequestDTO dto) {
        if (dto == null) return null;
        Reserva reserva = new Reserva();
        reserva.setUsuario(usuarioRepo.findById(dto.getCedulaUsuario())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado")));
        reserva.setFecha(dto.getFecha());
        reserva.setHoraInicio(dto.getHoraInicio());
        reserva.setNumPersonas(dto.getNumPersonas());
        return reserva;
    }

    // Método toResponseDTO: transforma entidad Reserva en ReservaResponseDTO mapeando usuario, mesa, fechas y estado

    public ReservaResponseDTO toResponseDTO(Reserva reserva) {
        if (reserva == null) return null;
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setIdReserva(reserva.getIdReserva());
        
        if (reserva.getUsuario() != null) {
            dto.setCedulaUsuario(reserva.getUsuario().getCedula());
            dto.setNombreUsuario(reserva.getUsuario().getNombre());
            dto.setEmailUsuario(reserva.getUsuario().getEmail());
        } else {
            dto.setCedulaUsuario(null);
            dto.setNombreUsuario(null);
            dto.setEmailUsuario(null);
        }
        
        if (reserva.getMesa() != null) {
            dto.setIdMesa(reserva.getMesa().getIdMesa());
            dto.setNumeroMesa(reserva.getMesa().getNumeroMesa());
            dto.setCapacidadMesa(reserva.getMesa().getCapacidad());
        } else {
            dto.setIdMesa(null);
            dto.setNumeroMesa(null);
            dto.setCapacidadMesa(null);
        }
        
        dto.setFecha(reserva.getFecha());
        dto.setHoraInicio(reserva.getHoraInicio());
        dto.setHoraFin(reserva.getHoraFin());
        dto.setNumPersonas(reserva.getNumPersonas());
        
        if (reserva.getEstadoReserva() != null) {
            dto.setEstadoReserva(reserva.getEstadoReserva().getNombre());
        } else {
            dto.setEstadoReserva(null);
        }
        
        return dto;
    }

    // Método toDto: convierte entidad Reserva a ReservaResponseDTO (similar a toResponseDTO, probablemente redundante)

    public ReservaResponseDTO toDto(Reserva reserva) {
        if (reserva == null) return null;
        
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setIdReserva(reserva.getIdReserva());
        dto.setFecha(reserva.getFecha());
        dto.setHoraInicio(reserva.getHoraInicio());
        dto.setHoraFin(reserva.getHoraFin());
        dto.setNumPersonas(reserva.getNumPersonas());

        if (reserva.getUsuario() != null) {
            dto.setCedulaUsuario(reserva.getUsuario().getCedula());
            dto.setNombreUsuario(reserva.getUsuario().getNombre());
            dto.setEmailUsuario(reserva.getUsuario().getEmail());
        }

        if (reserva.getMesa() != null) {
            dto.setIdMesa(reserva.getMesa().getIdMesa());
            dto.setNumeroMesa(reserva.getMesa().getNumeroMesa());
            dto.setCapacidadMesa(reserva.getMesa().getCapacidad());
        }

        if (reserva.getEstadoReserva() != null) {
            dto.setEstadoReserva(reserva.getEstadoReserva().getNombre());
        }
        
        return dto;
    }
}

