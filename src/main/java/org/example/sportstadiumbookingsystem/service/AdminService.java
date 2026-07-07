package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.admin.DashboardStatsResponse;
import org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus;
import org.example.sportstadiumbookingsystem.entityEnums.ReviewStatus;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.ReservationRepository;
import org.example.sportstadiumbookingsystem.repository.ReviewRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;

    public DashboardStatsResponse getDashboardStats() {
        BigDecimal completedRevenue = reservationRepository.sumTotalPriceByStatus(ReservationStatus.COMPLETED);
        BigDecimal confirmedRevenue = reservationRepository.sumTotalPriceByStatus(ReservationStatus.CONFIRMED);

        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalCustomers(userRepository.countByRole(UserRole.CUSTOMER))
                .totalStadiumOwners(userRepository.countByRole(UserRole.STADIUM_OWNER))
                .totalStadiums(stadiumRepository.count())
                .activeStadiums(stadiumRepository.countByStatus(StadiumStatus.ACTIVE))
                .pendingStadiums(stadiumRepository.countByStatus(StadiumStatus.PENDING_APPROVAL))
                .suspendedStadiums(stadiumRepository.countByStatus(StadiumStatus.SUSPENDED))
                .totalReservations(reservationRepository.count())
                .confirmedReservations(reservationRepository.countByStatus(ReservationStatus.CONFIRMED))
                .completedReservations(reservationRepository.countByStatus(ReservationStatus.COMPLETED))
                .cancelledReservations(reservationRepository.countByStatus(ReservationStatus.CANCELLED))
                .totalRevenue(completedRevenue.add(confirmedRevenue))
                .totalReviews(reviewRepository.count())
                .pendingReviews(reviewRepository.countByStatus(ReviewStatus.PENDING))
                .build();
    }
}