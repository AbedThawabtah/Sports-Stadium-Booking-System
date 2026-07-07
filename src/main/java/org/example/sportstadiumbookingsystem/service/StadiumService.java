package org.example.sportstadiumbookingsystem.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumRequest;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumResponse;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.specification.StadiumSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public StadiumResponse createStadium(StadiumRequest request) {
        User owner = getCurrentUser();

        Stadium stadium = Stadium.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .city(request.getCity())
                .sportType(request.getSportType())
                .capacity(request.getCapacity())
                .pricePerHour(request.getPricePerHour())
                .contactInfo(request.getContactInfo())
                .status(StadiumStatus.PENDING_APPROVAL)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();

        Stadium saved = stadiumRepository.save(stadium);
        return toResponse(saved);
    }

    public Page<StadiumResponse> searchStadiums(String keyword, String city, String sportType,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 LocalDate date, Pageable pageable) {
        Specification<Stadium> spec = StadiumSpecifications.hasStatus(StadiumStatus.ACTIVE);

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(StadiumSpecifications.matchesKeyword(keyword));
        }
        if (city != null && !city.isBlank()) {
            spec = spec.and(StadiumSpecifications.hasCity(city));
        }
        if (sportType != null && !sportType.isBlank()) {
            spec = spec.and(StadiumSpecifications.hasSportType(sportType));
        }
        if (minPrice != null) {
            spec = spec.and(StadiumSpecifications.hasMinPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(StadiumSpecifications.hasMaxPrice(maxPrice));
        }
        if (date != null) {
            spec = spec.and(StadiumSpecifications.hasAvailableSlotOn(date));
        }

        return stadiumRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public List<StadiumResponse> getMyStadiums() {
        User owner = getCurrentUser();
        return stadiumRepository.findByOwnerId(owner.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StadiumResponse getStadiumById(Long id) {
        Stadium stadium = findStadiumOrThrow(id);
        return toResponse(stadium);
    }

    public StadiumResponse updateStadium(Long id, StadiumRequest request) {
        Stadium stadium = findStadiumOrThrow(id);
        User currentUser = getCurrentUser();

        if (!stadium.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this stadium");
        }

        stadium.setName(request.getName());
        stadium.setDescription(request.getDescription());
        stadium.setLocation(request.getLocation());
        stadium.setCity(request.getCity());
        stadium.setSportType(request.getSportType());
        stadium.setCapacity(request.getCapacity());
        stadium.setPricePerHour(request.getPricePerHour());
        stadium.setContactInfo(request.getContactInfo());

        Stadium updated = stadiumRepository.save(stadium);
        return toResponse(updated);
    }

    public void deleteStadium(Long id) {
        Stadium stadium = findStadiumOrThrow(id);
        User currentUser = getCurrentUser();

        boolean isOwner = stadium.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        stadiumRepository.delete(stadium);
    }

    // ─── Helper methods ────────────────────────────────────────────

    private Stadium findStadiumOrThrow(Long id) {
        return stadiumRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stadium not found"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private StadiumResponse toResponse(Stadium stadium) {
        return StadiumResponse.builder()
                .id(stadium.getId())
                .name(stadium.getName())
                .description(stadium.getDescription())
                .location(stadium.getLocation())
                .city(stadium.getCity())
                .sportType(stadium.getSportType())
                .capacity(stadium.getCapacity())
                .pricePerHour(stadium.getPricePerHour())
                .contactInfo(stadium.getContactInfo())
                .status(stadium.getStatus())
                .averageRating(stadium.getAverageRating() != null ? stadium.getAverageRating().doubleValue() : 0.0)
                .totalReviews(stadium.getTotalReviews())
                .createdAt(stadium.getCreatedAt())
                .ownerId(stadium.getOwner().getId())
                .ownerName(stadium.getOwner().getFullName())
                .build();
    }

    // ─── Admin approval flow ────────────────────────────────────────

    @Transactional
    public StadiumResponse approveStadium(Long id) {
        Stadium stadium = findStadiumOrThrow(id);

        if (stadium.getStatus() != StadiumStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only stadiums pending approval can be approved (current status: " + stadium.getStatus() + ")");
        }

        stadium.setStatus(StadiumStatus.ACTIVE);
        Stadium saved = stadiumRepository.save(stadium);

        activityLogService.log(getCurrentUser(), "STADIUM_APPROVED", "Stadium", saved.getId(),
                "Stadium '" + saved.getName() + "' approved and made active");

        return toResponse(saved);
    }

    @Transactional
    public StadiumResponse suspendStadium(Long id) {
        Stadium stadium = findStadiumOrThrow(id);

        if (stadium.getStatus() == StadiumStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stadium is already suspended");
        }

        stadium.setStatus(StadiumStatus.SUSPENDED);
        Stadium saved = stadiumRepository.save(stadium);

        activityLogService.log(getCurrentUser(), "STADIUM_SUSPENDED", "Stadium", saved.getId(),
                "Stadium '" + saved.getName() + "' suspended");

        return toResponse(saved);
    }

    @Transactional
    public StadiumResponse reactivateStadium(Long id) {
        Stadium stadium = findStadiumOrThrow(id);

        if (stadium.getStatus() != StadiumStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only suspended stadiums can be reactivated");
        }

        stadium.setStatus(StadiumStatus.ACTIVE);
        Stadium saved = stadiumRepository.save(stadium);

        activityLogService.log(getCurrentUser(), "STADIUM_REACTIVATED", "Stadium", saved.getId(),
                "Stadium '" + saved.getName() + "' reactivated");

        return toResponse(saved);
    }

    public List<StadiumResponse> getPendingStadiums() {
        return stadiumRepository.findByStatus(StadiumStatus.PENDING_APPROVAL)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}