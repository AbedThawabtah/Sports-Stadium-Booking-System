package org.example.sportstadiumbookingsystem.dto.timeslot;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GenerateTimeSlotsRangeRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 15, message = "Slot duration must be at least 15 minutes")
    private Integer slotDurationMinutes = 60;
}