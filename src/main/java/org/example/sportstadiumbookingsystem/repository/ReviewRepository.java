package org.example.sportstadiumbookingsystem.repository;


import org.example.sportstadiumbookingsystem.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStadiumId(Long stadiumId);
    boolean existsByReservationId(Long reservationId);
}
