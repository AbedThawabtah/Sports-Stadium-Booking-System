package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.auth.AuthResponse;
import org.example.sportstadiumbookingsystem.dto.auth.LoginRequest;
import org.example.sportstadiumbookingsystem.dto.auth.RefreshTokenRequest;
import org.example.sportstadiumbookingsystem.dto.auth.RegisterRequest;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(resolveSelfRegistrationRole(request.getRole()))
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    private UserRole resolveSelfRegistrationRole(UserRole requestedRole) {
        if (requestedRole == null) {
            return UserRole.CUSTOMER;
        }
        if (requestedRole == UserRole.CUSTOMER || requestedRole == UserRole.STADIUM_OWNER) {
            return requestedRole;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role for self-registration");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        String email;
        try {
            email = jwtService.extractEmail(refreshToken);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        UserDetails userDetails = buildUserDetails(user);

        boolean isValid = jwtService.isRefreshToken(refreshToken)
                && jwtService.isTokenValid(refreshToken, userDetails);

        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = buildUserDetails(user);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.getIsActive())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}