package org.example.sportstadiumbookingsystem.repository;


import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByStadiumIdAndSlotDate(Long stadiumId, LocalDate slotDate);
    List<TimeSlot> findByStadiumIdAndSlotDateAndStatus(Long stadiumId, LocalDate slotDate, SlotStatus status);
}
