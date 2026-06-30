package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.auth.AuthResponse;
import org.example.sportstadiumbookingsystem.dto.auth.LoginRequest;
import org.example.sportstadiumbookingsystem.dto.auth.RefreshTokenRequest;
import org.example.sportstadiumbookingsystem.dto.auth.RegisterRequest;
import org.example.sportstadiumbookingsystem.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}