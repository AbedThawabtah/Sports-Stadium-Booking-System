package org.example.sportstadiumbookingsystem.dto.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationRequest {

    @NotNull(message = "Time slot id is required")
    private Long timeSlotId;
}