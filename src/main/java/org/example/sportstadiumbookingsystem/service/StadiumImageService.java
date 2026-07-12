package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.stadiumimage.StadiumImageResponse;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.StadiumImage;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.repository.StadiumImageRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StadiumImageService {

    private static final int MAX_IMAGES_PER_STADIUM = 10;

    private final StadiumImageRepository stadiumImageRepository;
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ActivityLogService activityLogService;
    private final StadiumService stadiumService;

    @Transactional
    public List<StadiumImageResponse> uploadImages(Long stadiumId, List<MultipartFile> files) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        User currentUser = getCurrentUser();
        checkOwnership(stadium, currentUser);

        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }

        long existingCount = stadiumImageRepository.countByStadiumId(stadiumId);
        if (existingCount + files.size() > MAX_IMAGES_PER_STADIUM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A stadium cannot have more than " + MAX_IMAGES_PER_STADIUM + " images");
        }

        boolean hasPrimaryAlready = stadiumImageRepository.findByStadiumIdAndIsPrimaryTrue(stadiumId).isPresent();

        List<StadiumImage> saved = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            String url = fileStorageService.store(files.get(i));
            boolean isPrimary = !hasPrimaryAlready && i == 0;

            StadiumImage image = StadiumImage.builder()
                    .stadium(stadium)
                    .imageUrl(url)
                    .isPrimary(isPrimary)
                    .build();

            saved.add(stadiumImageRepository.save(image));
        }

        activityLogService.log(currentUser, "STADIUM_IMAGES_UPLOADED", "Stadium", stadium.getId(),
                saved.size() + " image(s) uploaded for stadium '" + stadium.getName() + "'");

        return saved.stream().map(this::toResponse).toList();
    }

    public List<StadiumImageResponse> getImages(Long stadiumId) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        stadiumService.assertStadiumViewable(stadium);
        return stadiumImageRepository.findByStadiumIdOrderByIsPrimaryDescCreatedAtAsc(stadiumId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StadiumImageResponse setPrimaryImage(Long stadiumId, Long imageId) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        User currentUser = getCurrentUser();
        checkOwnership(stadium, currentUser);

        StadiumImage target = findImageOrThrow(imageId, stadiumId);

        stadiumImageRepository.findByStadiumIdAndIsPrimaryTrue(stadiumId)
                .ifPresent(current -> {
                    current.setIsPrimary(false);
                    stadiumImageRepository.save(current);
                });

        target.setIsPrimary(true);
        StadiumImage saved = stadiumImageRepository.save(target);
        return toResponse(saved);
    }

    @Transactional
    public void deleteImage(Long stadiumId, Long imageId) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        User currentUser = getCurrentUser();
        checkOwnership(stadium, currentUser);

        StadiumImage image = findImageOrThrow(imageId, stadiumId);
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        fileStorageService.delete(image.getImageUrl());
        stadiumImageRepository.delete(image);

        // إذا حذفنا الصورة الرئيسية، نخلي أقدم صورة متبقية هي الرئيسية
        if (wasPrimary) {
            stadiumImageRepository.findByStadiumIdOrderByIsPrimaryDescCreatedAtAsc(stadiumId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        stadiumImageRepository.save(next);
                    });
        }
    }

    // ─── Helper methods ────────────────────────────────────────────

    private Stadium findStadiumOrThrow(Long stadiumId) {
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stadium not found"));
    }

    private StadiumImage findImageOrThrow(Long imageId, Long stadiumId) {
        StadiumImage image = stadiumImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        if (!image.getStadium().getId().equals(stadiumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image does not belong to this stadium");
        }
        return image;
    }

    private void checkOwnership(Stadium stadium, User currentUser) {
        if (!stadium.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this stadium");
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private StadiumImageResponse toResponse(StadiumImage image) {
        return StadiumImageResponse.builder()
                .id(image.getId())
                .stadiumId(image.getStadium().getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .createdAt(image.getCreatedAt())
                .build();
    }
}