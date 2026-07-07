package org.example.sportstadiumbookingsystem.dto.workinghour;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek;

import java.time.LocalTime;

@Getter
@Setter
public class WorkingHourRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    // لو isClosed = true، ممكن يكونوا null
    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean isClosed = false;
}