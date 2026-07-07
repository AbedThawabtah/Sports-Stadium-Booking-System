package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.review.ReviewRequest;
import org.example.sportstadiumbookingsystem.dto.review.ReviewResponse;
import org.example.sportstadiumbookingsystem.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STADIUM_OWNER')")
    public ResponseEntity<ReviewResponse> submitReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/reviews/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getMyReviews());
    }

    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteMyReview(@PathVariable Long id) {
        reviewService.deleteMyReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/stadiums/{stadiumId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getStadiumReviews(@PathVariable Long stadiumId) {
        return ResponseEntity.ok(reviewService.getStadiumReviews(stadiumId));
    }
}