package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.reservation.CancelReservationRequest;
import org.example.sportstadiumbookingsystem.dto.reservation.ReservationRequest;
import org.example.sportstadiumbookingsystem.dto.reservation.ReservationResponse;
import org.example.sportstadiumbookingsystem.entity.Reservation;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.ReservationRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.TimeSlotRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    // ما بنسمح بإلغاء الحجز إذا بقي أقل من ساعتين على موعد البداية
    private static final long CANCELLATION_WINDOW_HOURS = 2;

    private final ReservationRepository reservationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        User customer = getCurrentUser();

        TimeSlot slot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time slot not found"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This time slot is already booked");
        }

        // مالك الملعب ما بيقدر يحجز ملعبه هو بالذات (بيقدر يحجز أي ملعب غيره عادي)
        if (slot.getStadium().getOwner().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot book your own stadium");
        }

        LocalDateTime slotStart = LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
        if (slotStart.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book a time slot in the past");
        }

        // نقفل السلوت أولاً؛ الحماية الحقيقية من الحجز المزدوج المتزامن هي الـ unique constraint على time_slot_id
        slot.setStatus(SlotStatus.BOOKED);
        timeSlotRepository.save(slot);

        Reservation reservation = Reservation.builder()
                .customer(customer)
                .timeSlot(slot)
                .stadium(slot.getStadium())
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(slot.getPrice())
                .reservationDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .build();

        Reservation saved;
        try {
            saved = reservationRepository.save(reservation);
            reservationRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This time slot has just been booked by someone else");
        }

        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id, CancelReservationRequest request) {
        Reservation reservation = findReservationOrThrow(id);
        User currentUser = getCurrentUser();

        boolean isOwner = reservation.getCustomer().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only confirmed reservations can be cancelled (current status: " + reservation.getStatus() + ")");
        }

        LocalDateTime slotStart = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
        long hoursUntilStart = Duration.between(LocalDateTime.now(), slotStart).toHours();

        if (!isAdmin && hoursUntilStart < CANCELLATION_WINDOW_HOURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel a reservation less than " + CANCELLATION_WINDOW_HOURS + " hours before start time");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancellationReason(request != null ? request.getReason() : null);

        TimeSlot slot = reservation.getTimeSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        timeSlotRepository.save(slot);

        Reservation saved = reservationRepository.save(reservation);
        return toResponse(saved);
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = findReservationOrThrow(id);
        User currentUser = getCurrentUser();

        boolean isCustomer = reservation.getCustomer().getId().equals(currentUser.getId());
        boolean isStadiumOwner = reservation.getStadium().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isCustomer && !isStadiumOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this reservation");
        }

        return toResponse(reservation);
    }

    public List<ReservationResponse> getMyReservations() {
        User currentUser = getCurrentUser();
        return reservationRepository.findByCustomerIdOrderByReservationDateDescStartTimeDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservationResponse> getStadiumReservations(Long stadiumId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stadium not found"));

        User currentUser = getCurrentUser();
        boolean isOwner = stadium.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this stadium");
        }

        return reservationRepository.findByStadiumIdOrderByReservationDateDescStartTimeDesc(stadiumId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Helper methods ────────────────────────────────────────────

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getFullName())
                .stadiumId(r.getStadium().getId())
                .stadiumName(r.getStadium().getName())
                .timeSlotId(r.getTimeSlot().getId())
                .status(r.getStatus())
                .totalPrice(r.getTotalPrice())
                .reservationDate(r.getReservationDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .cancelledAt(r.getCancelledAt())
                .cancellationReason(r.getCancellationReason())
                .createdAt(r.getCreatedAt())
                .build();
    }

    // ─── Auto-completion (يستدعيها الـ Scheduler) ────────────────

    @Transactional
    public void autoCompletePastReservations() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Reservation> due = reservationRepository.findConfirmedReservationsPastEndTime(today, now);
        if (due.isEmpty()) {
            return;
        }

        due.forEach(r -> r.setStatus(ReservationStatus.COMPLETED));
        reservationRepository.saveAll(due);
    }
}