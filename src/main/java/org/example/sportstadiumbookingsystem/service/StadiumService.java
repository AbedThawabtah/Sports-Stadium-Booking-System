package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumRequest;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumResponse;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;

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
                .build();

        Stadium saved = stadiumRepository.save(stadium);
        return toResponse(saved);
    }

    public List<StadiumResponse> getAllActiveStadiums() {
        return stadiumRepository.findAll()
                .stream()
                .filter(s -> s.getStatus() == StadiumStatus.ACTIVE)
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
}