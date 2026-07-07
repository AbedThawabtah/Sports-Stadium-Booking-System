package org.example.sportstadiumbookingsystem.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DashboardStatsResponse {

    private long totalUsers;
    private long totalCustomers;
    private long totalStadiumOwners;

    private long totalStadiums;
    private long activeStadiums;
    private long pendingStadiums;
    private long suspendedStadiums;

    private long totalReservations;
    private long confirmedReservations;
    private long completedReservations;
    private long cancelledReservations;
    private BigDecimal totalRevenue;

    private long totalReviews;
    private long pendingReviews;
}