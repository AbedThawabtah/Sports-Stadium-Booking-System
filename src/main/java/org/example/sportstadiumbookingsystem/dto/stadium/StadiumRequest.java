package org.example.sportstadiumbookingsystem.dto.stadium;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StadiumRequest {

    @NotBlank(message = "Stadium name is required")
    private String name;

    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Sport type is required")
    private String sportType;

    @Positive(message = "Capacity must be a positive number")
    private Integer capacity;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    private String contactInfo;
}