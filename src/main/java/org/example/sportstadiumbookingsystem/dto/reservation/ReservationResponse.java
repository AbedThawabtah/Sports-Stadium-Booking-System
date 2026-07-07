package org.example.sportstadiumbookingsystem.dto.reservation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ReservationResponse {

    private Long id;

    private Long customerId;
    private String customerName;

    private Long stadiumId;
    private String stadiumName;

    private Long timeSlotId;

    private ReservationStatus status;
    private BigDecimal totalPrice;

    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private LocalDateTime cancelledAt;
    private String cancellationReason;

    private LocalDateTime createdAt;
}