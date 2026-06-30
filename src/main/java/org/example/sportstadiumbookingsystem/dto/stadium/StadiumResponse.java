package org.example.sportstadiumbookingsystem.dto.stadium;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StadiumResponse {

    private Long id;
    private String name;
    private String description;
    private String location;
    private String city;
    private String sportType;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private String contactInfo;
    private StadiumStatus status;
    private Double averageRating;
    private Integer totalReviews;
    private LocalDateTime createdAt;

    // Owner info — no full User object exposed
    private Long ownerId;
    private String ownerName;
}