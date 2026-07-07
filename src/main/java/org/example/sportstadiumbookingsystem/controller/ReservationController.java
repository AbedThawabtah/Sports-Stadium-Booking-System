package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.reservation.CancelReservationRequest;
import org.example.sportstadiumbookingsystem.dto.reservation.ReservationRequest;
import org.example.sportstadiumbookingsystem.dto.reservation.ReservationResponse;
import org.example.sportstadiumbookingsystem.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/api/reservations")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STADIUM_OWNER')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/reservations/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        List<ReservationResponse> response = reservationService.getMyReservations();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/reservations/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/reservations/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) CancelReservationRequest request) {

        ReservationResponse response = reservationService.cancelReservation(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/stadiums/{stadiumId}/reservations")
    @PreAuthorize("hasRole('STADIUM_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getStadiumReservations(@PathVariable Long stadiumId) {
        List<ReservationResponse> response = reservationService.getStadiumReservations(stadiumId);
        return ResponseEntity.ok(response);
    }
}