package org.example.sportstadiumbookingsystem.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String refreshToken;
    private Long userId;
    private String fullName;
    private String email;
    private UserRole role;
}