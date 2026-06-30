package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCustomerId(Long customerId);
    List<Reservation> findByStadiumId(Long stadiumId);
}
