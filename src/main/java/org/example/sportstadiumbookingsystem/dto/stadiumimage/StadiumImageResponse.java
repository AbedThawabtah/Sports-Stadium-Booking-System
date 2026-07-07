package org.example.sportstadiumbookingsystem.dto.stadiumimage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StadiumImageResponse {

    private Long id;
    private Long stadiumId;
    private String imageUrl;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}