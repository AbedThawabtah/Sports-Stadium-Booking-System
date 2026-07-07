package org.example.sportstadiumbookingsystem.dto.timeslot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class TimeSlotResponse {

    private Long id;
    private Long stadiumId;
    private String stadiumName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SlotStatus status;
    private BigDecimal price;
}