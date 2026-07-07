package org.example.sportstadiumbookingsystem.dto.workinghour;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class WorkingHourResponse {

    private Long id;
    private DayOfWeek dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean isClosed;
}