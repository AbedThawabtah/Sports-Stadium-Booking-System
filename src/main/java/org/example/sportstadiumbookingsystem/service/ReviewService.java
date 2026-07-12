package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.review.ReviewModerationRequest;
import org.example.sportstadiumbookingsystem.dto.review.ReviewRequest;
import org.example.sportstadiumbookingsystem.dto.review.ReviewResponse;
import org.example.sportstadiumbookingsystem.entity.Reservation;
import org.example.sportstadiumbookingsystem.entity.Review;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus;
import org.example.sportstadiumbookingsystem.entityEnums.ReviewStatus;
import org.example.sportstadiumbookingsystem.repository.ReservationRepository;
import org.example.sportstadiumbookingsystem.repository.ReviewRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    // يسمح للزبون يعدل/يحذف تقييمه بنفسه خلال 24 ساعة من الإرسال وقبل موافقة الأدمن فقط
    private static final long SELF_EDIT_WINDOW_HOURS = 24;

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final StadiumService stadiumService;

    @Transactional
    public ReviewResponse submitReview(ReviewRequest request) {
        User customer = getCurrentUser();

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        if (!reservation.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review your own reservations");
        }

        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can only review a reservation after it has been completed");
        }

        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this reservation");
        }

        Review review = Review.builder()
                .customer(customer)
                .stadium(reservation.getStadium())
                .reservation(reservation)
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.PENDING)
                .build();

        Review saved = reviewRepository.save(review);

        activityLogService.log(customer, "REVIEW_SUBMITTED", "Review", saved.getId(),
                "Review submitted for stadium '" + reservation.getStadium().getName() + "'");

        return toResponse(saved);
    }

    public List<ReviewResponse> getMyReviews() {
        User customer = getCurrentUser();
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // عام: أي زائر يقدر يشوف التقييمات المعتمدة فقط لملعب معين
    public List<ReviewResponse> getStadiumReviews(Long stadiumId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stadium not found"));
        stadiumService.assertStadiumViewable(stadium);

        return reviewRepository.findByStadiumIdAndStatusOrderByCreatedAtDesc(stadiumId, ReviewStatus.APPROVED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // للأدمن فقط: التقييمات المعلّقة بانتظار الموافقة
    public List<ReviewResponse> getPendingReviews() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse approveReview(Long id) {
        Review review = findReviewOrThrow(id);
        User admin = getCurrentUser();

        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot approve a deleted review");
        }

        review.setStatus(ReviewStatus.APPROVED);
        Review saved = reviewRepository.save(review);

        recalculateStadiumRating(saved.getStadium());

        activityLogService.log(admin, "REVIEW_APPROVED", "Review", saved.getId(),
                "Review approved for stadium '" + saved.getStadium().getName() + "'");

        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse rejectReview(Long id, ReviewModerationRequest request) {
        Review review = findReviewOrThrow(id);
        User admin = getCurrentUser();

        boolean wasApproved = review.getStatus() == ReviewStatus.APPROVED;

        review.setStatus(ReviewStatus.DELETED);
        Review saved = reviewRepository.save(review);

        if (wasApproved) {
            recalculateStadiumRating(saved.getStadium());
        }

        String reason = (request != null && request.getReason() != null) ? ": " + request.getReason() : "";
        activityLogService.log(admin, "REVIEW_REJECTED", "Review", saved.getId(),
                "Review moderated/removed by admin" + reason);

        return toResponse(saved);
    }

    @Transactional
    public void deleteMyReview(Long id) {
        Review review = findReviewOrThrow(id);
        User currentUser = getCurrentUser();

        if (!review.getCustomer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own review");
        }

        if (!isEditable(review)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This review can no longer be edited or deleted");
        }

        boolean wasApproved = review.getStatus() == ReviewStatus.APPROVED;
        Stadium stadium = review.getStadium();

        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);

        if (wasApproved) {
            recalculateStadiumRating(stadium);
        }
    }

    // ─── Helper methods ────────────────────────────────────────────

    // يقابل isEditable() بالـ Class Diagram: بس التقييمات المعلّقة وخلال أول 24 ساعة
    private boolean isEditable(Review review) {
        return review.getStatus() == ReviewStatus.PENDING
                && review.getCreatedAt() != null
                && review.getCreatedAt().isAfter(LocalDateTime.now().minusHours(SELF_EDIT_WINDOW_HOURS));
    }

    // يقابل recalculateRating() بالـ Class Diagram: بيحسب المعدل من التقييمات المعتمدة فقط
    private void recalculateStadiumRating(Stadium stadium) {
        List<Review> approved = reviewRepository.findByStadiumIdAndStatusOrderByCreatedAtDesc(
                stadium.getId(), ReviewStatus.APPROVED);

        if (approved.isEmpty()) {
            stadium.setAverageRating(BigDecimal.ZERO);
            stadium.setTotalReviews(0);
        } else {
            double average = approved.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            stadium.setAverageRating(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
            stadium.setTotalReviews(approved.size());
        }

        stadiumRepository.save(stadium);
    }

    private Review findReviewOrThrow(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .customerId(review.getCustomer().getId())
                .customerName(review.getCustomer().getFullName())
                .stadiumId(review.getStadium().getId())
                .stadiumName(review.getStadium().getName())
                .reservationId(review.getReservation().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}