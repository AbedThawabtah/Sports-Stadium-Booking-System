package org.example.sportstadiumbookingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.activitylog.ActivityLogResponse;
import org.example.sportstadiumbookingsystem.dto.admin.DashboardStatsResponse;
import org.example.sportstadiumbookingsystem.dto.admin.UserSummaryResponse;
import org.example.sportstadiumbookingsystem.dto.review.ReviewModerationRequest;
import org.example.sportstadiumbookingsystem.dto.review.ReviewResponse;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumResponse;
import org.example.sportstadiumbookingsystem.service.ActivityLogService;
import org.example.sportstadiumbookingsystem.service.AdminService;
import org.example.sportstadiumbookingsystem.service.ReviewService;
import org.example.sportstadiumbookingsystem.service.StadiumService;
import org.example.sportstadiumbookingsystem.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StadiumService stadiumService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final ActivityLogService activityLogService;
    private final AdminService adminService;

    // ─── Stadium approval ────────────────────────────────────────

    @GetMapping("/stadiums/pending")
    public ResponseEntity<List<StadiumResponse>> getPendingStadiums() {
        return ResponseEntity.ok(stadiumService.getPendingStadiums());
    }

    @PutMapping("/stadiums/{id}/approve")
    public ResponseEntity<StadiumResponse> approveStadium(@PathVariable Long id) {
        return ResponseEntity.ok(stadiumService.approveStadium(id));
    }

    @PutMapping("/stadiums/{id}/suspend")
    public ResponseEntity<StadiumResponse> suspendStadium(@PathVariable Long id) {
        return ResponseEntity.ok(stadiumService.suspendStadium(id));
    }

    @PutMapping("/stadiums/{id}/reactivate")
    public ResponseEntity<StadiumResponse> reactivateStadium(@PathVariable Long id) {
        return ResponseEntity.ok(stadiumService.reactivateStadium(id));
    }

    // ─── User management ────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<UserSummaryResponse> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    // ─── Review moderation ────────────────────────────────────────

    @GetMapping("/reviews/pending")
    public ResponseEntity<List<ReviewResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/reviews/{id}/approve")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PutMapping("/reviews/{id}/reject")
    public ResponseEntity<ReviewResponse> rejectReview(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewModerationRequest request) {
        return ResponseEntity.ok(reviewService.rejectReview(id, request));
    }

    // ─── Activity log ────────────────────────────────────────

    @GetMapping("/activity-logs")
    public ResponseEntity<List<ActivityLogResponse>> getAllActivityLogs() {
        return ResponseEntity.ok(activityLogService.getAllLogs());
    }

    @GetMapping("/activity-logs/user/{userId}")
    public ResponseEntity<List<ActivityLogResponse>> getUserActivityLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(activityLogService.getLogsForUser(userId));
    }

    // ─── Dashboard ────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }
}