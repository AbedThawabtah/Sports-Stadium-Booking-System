package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.admin.UserSummaryResponse;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserSummaryResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        User currentAdmin = getCurrentUser();
        if (user.getId().equals(currentAdmin.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }

        user.setIsActive(!user.getIsActive());
        User saved = userRepository.save(user);

        activityLogService.log(currentAdmin,
                saved.getIsActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "User", saved.getId(),
                "User '" + saved.getEmail() + "' status changed to " + (saved.getIsActive() ? "ACTIVE" : "INACTIVE"));

        return toResponse(saved);
    }

    // ─── Helper methods ────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private UserSummaryResponse toResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}