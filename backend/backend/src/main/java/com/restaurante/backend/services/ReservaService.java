package com.restaurante.backend.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.restaurante.backend.entities.EstadoReserva;
import com.restaurante.backend.entities.Mesa;
import com.restaurante.backend.entities.Pago;
import com.restaurante.backend.entities.Reserva;
import com.restaurante.backend.repositories.EstadoReservaRepository;
import com.restaurante.backend.repositories.MesaRepository;
import com.restaurante.backend.repositories.PagoRepository;
import com.restaurante.backend.repositories.ReservaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final PagoRepository pagoRepo;
    private final MesaRepository mesaRepo;
    private final EstadoReservaRepository estadoRepo;

    private final EmailService emailService;

    @Transactional
    public Reserva crearReserva(Reserva reserva) {
        // 1. Calcular hora fin (Inicio + 2 horas)
        reserva.setHoraFin(reserva.getHoraInicio().plusHours(2));

        // 2. Buscar mesas que tengan capacidad suficiente
        List<Mesa> mesasCandidatas = mesaRepo.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(reserva.getNumPersonas());

        // 3. Encontrar la primera mesa disponible en ese rango
        Mesa mesaAsignada = mesasCandidatas.stream()
            .filter(m -> reservaRepo.findOverlappingReservations(m.getIdMesa(), reserva.getFecha(), 
                                                                reserva.getHoraInicio(), reserva.getHoraFin()).isEmpty())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hay mesas disponibles para " + reserva.getNumPersonas() + " personas en ese horario."));

        reserva.setMesa(mesaAsignada);
        Reserva guardada = reservaRepo.save(reserva);

        // 4. Enviar correo de confirmación de creación
        emailService.enviarCorreo(
            guardada.getUsuario().getEmail(),
            "Reserva Recibida - Novost",
            "Hola " + guardada.getUsuario().getNombre() + ", su reserva ha sido registrada.\n" +
            "Recuerde que tiene hasta 24 horas antes del " + guardada.getFecha() + " para pagar, de lo contrario se cancelará."
        );

        return guardada;
    }

    @Transactional
    public Pago procesarPagoReserva(Long idReserva, String idPasarela, Double monto) {
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setIdPasarela(idPasarela);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setIdEstadoPago("COMPLETADO"); // Respuesta de la pasarela

        return pagoRepo.save(pago);
    }

    @Transactional
    public void cancelarReserva(Long idReserva) {
        Reserva reserva = reservaRepo.findById(idReserva)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // 1. Verificar si tiene un pago asociado
        Optional<Pago> pagoOpt = pagoRepo.findByReserva(reserva);
        
        if (pagoOpt.isPresent()) {
            Pago pago = pagoOpt.get();
            // Lógica ficticia: Llamada a la API de la pasarela usando pago.getIdPasarela()
            boolean reembolsoExitoso = simularReembolsoPasarela(pago.getIdPasarela(), pago.getMonto());
            
            if (!reembolsoExitoso) {
                throw new RuntimeException("Error al procesar la devolución con la pasarela de pago");
            }
            // Opcional: Marcar el pago como "REEMBOLSADO"
            pago.setIdEstadoPago("REEMBOLSADO");
            pagoRepo.save(pago);
        }

        // 2. Cambiar estado de la reserva a "CANCELADA"
        EstadoReserva estadoCancelado = estadoRepo.findByNombre("CANCELADA")
                .orElseThrow(() -> new RuntimeException("Estado CANCELADA no configurado"));
        
        reserva.setEstadoReserva(estadoCancelado);
        reservaRepo.save(reserva);
    }

    private boolean simularReembolsoPasarela(String idPasarela, Double monto) {
        // Aquí iría la integración real (RestTemplate/WebClient) hacia Stripe/PayPal
        System.out.println("Solicitando reembolso de " + monto + " para el ID: " + idPasarela);
        return true; 
    }
}