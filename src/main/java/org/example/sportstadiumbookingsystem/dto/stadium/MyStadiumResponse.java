package org.example.sportstadiumbookingsystem.dto.stadium;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class MyStadiumResponse {

    private Long id;
    private String name;
    private String city;
    private String sportType;
    private BigDecimal pricePerHour;
    private StadiumStatus status;
    private Double averageRating;
    private Integer totalReviews;

    private long totalReservations;
    private long confirmedReservations;
    private long cancelledReservations;
    private long completedReservations;
}
