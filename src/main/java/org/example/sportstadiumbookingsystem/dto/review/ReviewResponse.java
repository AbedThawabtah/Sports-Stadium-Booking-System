package org.example.sportstadiumbookingsystem.dto.review;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.ReviewStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReviewResponse {

    private Long id;

    private Long customerId;
    private String customerName;

    private Long stadiumId;
    private String stadiumName;

    private Long reservationId;

    private Integer rating;
    private String comment;
    private ReviewStatus status;

    private LocalDateTime createdAt;
}