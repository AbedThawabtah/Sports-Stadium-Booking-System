package org.example.sportstadiumbookingsystem.repository;

import org.example.sportstadiumbookingsystem.entity.Reservation;
import org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCustomerIdOrderByReservationDateDescStartTimeDesc(Long customerId);
    List<Reservation> findByStadiumIdOrderByReservationDateDescStartTimeDesc(Long stadiumId);
    boolean existsByTimeSlotId(Long timeSlotId);

    long countByStatus(ReservationStatus status);

    long countByStadiumId(Long stadiumId);

    long countByStadiumIdAndStatus(Long stadiumId, ReservationStatus status);

    @Query("select coalesce(sum(r.totalPrice), 0) from Reservation r where r.status = :status")
    BigDecimal sumTotalPriceByStatus(@Param("status") ReservationStatus status);

    // الحجوزات المؤكدة اللي انتهى وقتها فعلياً (تاريخها فات، أو نفس اليوم ووقت النهاية فات)
    @Query("select r from Reservation r where r.status = org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus.CONFIRMED " +
            "and (r.reservationDate < :today or (r.reservationDate = :today and r.endTime <= :now))")
    List<Reservation> findConfirmedReservationsPastEndTime(@Param("today") LocalDate today, @Param("now") LocalTime now);
}