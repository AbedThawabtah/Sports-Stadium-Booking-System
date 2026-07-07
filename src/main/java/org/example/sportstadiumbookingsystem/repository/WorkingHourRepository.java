package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.WorkingHour;
import org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, Long> {
    List<WorkingHour> findByStadiumIdOrderByDayOfWeekAsc(Long stadiumId);
    void deleteByStadiumId(Long stadiumId);
    Optional<WorkingHour> findByStadiumIdAndDayOfWeek(Long stadiumId, DayOfWeek dayOfWeek);
}