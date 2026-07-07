package org.example.sportstadiumbookingsystem.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserSummaryResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}