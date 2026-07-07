package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.Review;
import org.example.sportstadiumbookingsystem.entityEnums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStadiumId(Long stadiumId);
    List<Review> findByStadiumIdAndStatusOrderByCreatedAtDesc(Long stadiumId, ReviewStatus status);
    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
    Optional<Review> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
    long countByStatus(ReviewStatus status);
}