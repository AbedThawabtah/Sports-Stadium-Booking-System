package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, Long> {
    List<WorkingHour> findByStadiumId(Long stadiumId);
}
