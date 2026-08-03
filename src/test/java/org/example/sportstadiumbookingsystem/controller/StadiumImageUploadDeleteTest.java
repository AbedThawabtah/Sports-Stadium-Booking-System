package org.example.sportstadiumbookingsystem.controller;

import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.StadiumImage;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.ActivityLogRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumImageRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intentionally NOT @Transactional: these tests verify that the service's own
 * @Transactional boundary really commits/rolls back on its own. If this class were
 * wrapped in an outer test transaction, the service transaction would just join it
 * (same thread, propagation REQUIRED), so a "rolled back" read here would actually
 * still see the not-yet-really-rolled-back rows. Cleanup is therefore manual.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StadiumImageUploadDeleteTest {

    private static final String OWNER_EMAIL = "owner-images@test.com";

    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10,
            'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xD9
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StadiumImageRepository stadiumImageRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final List<String> storedUrls = new ArrayList<>();
    private final List<Long> createdStadiumIds = new ArrayList<>();
    private final List<Long> createdOwnerIds = new ArrayList<>();

    // This class deliberately runs test bodies without an ambient transaction (see class
    // javadoc). @Transactional on an @AfterEach method has no effect (JUnit invokes lifecycle
    // methods directly, bypassing any Spring proxy), so cleanup uses TransactionTemplate to
    // open its own real transaction imperatively.
    @AfterEach
    void cleanUp() {
        storedUrls.forEach(url -> {
            try {
                fileStorageService.delete(url);
            } catch (Exception ignored) {
                // best-effort cleanup of test fixture files
            }
        });
        storedUrls.clear();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            createdStadiumIds.forEach(id -> {
                stadiumImageRepository.deleteByStadiumId(id);
                stadiumRepository.deleteById(id);
            });
            createdOwnerIds.forEach(ownerId -> {
                activityLogRepository.deleteAll(activityLogRepository.findByUserId(ownerId));
                userRepository.deleteById(ownerId);
            });
        });
        createdStadiumIds.clear();
        createdOwnerIds.clear();
    }

    private Path uploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private long countFilesOnDisk() throws IOException {
        Path dir = uploadPath();
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.count();
        }
    }

    // Windows (with real-time AV scanning) can briefly still report a just-deleted
    // file in a directory listing, so poll rather than asserting on a single snapshot.
    private void assertFileCountEventually(long expected) throws Exception {
        long actual = countFilesOnDisk();
        for (int i = 0; i < 40 && actual != expected; i++) {
            Thread.sleep(50);
            actual = countFilesOnDisk();
        }
        assertEquals(expected, actual);
    }

    private void assertFileGoneEventually(Path path) throws InterruptedException {
        boolean exists = Files.exists(path);
        for (int i = 0; i < 40 && exists; i++) {
            Thread.sleep(50);
            exists = Files.exists(path);
        }
        assertFalse(exists);
    }

    private User createOwner(String email) {
        User owner = userRepository.save(User.builder()
                .fullName(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());
        createdOwnerIds.add(owner.getId());
        return owner;
    }

    private Stadium createStadium(User owner, String name) {
        Stadium stadium = stadiumRepository.save(Stadium.builder()
                .owner(owner)
                .name(name)
                .location("Loc")
                .city("Ramallah")
                .sportType("Football")
                .pricePerHour(new BigDecimal("50.00"))
                .status(StadiumStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());
        createdStadiumIds.add(stadium.getId());
        return stadium;
    }

    private StadiumImage createImageDirectly(Stadium stadium, boolean primary) {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);
        String url = fileStorageService.store(file);
        storedUrls.add(url);
        return stadiumImageRepository.save(StadiumImage.builder()
                .stadium(stadium)
                .imageUrl(url)
                .isPrimary(primary)
                .build());
    }

    private Path resolveFile(String imageUrl) {
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        return uploadPath().resolve(filename);
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void thirdFileFailingValidation_rollsBackAllFilesAndAllDbRowsFromThatCall() throws Exception {
        User owner = createOwner(OWNER_EMAIL);
        Stadium stadium = createStadium(owner, "Rollback Stadium");

        long filesBefore = countFilesOnDisk();

        MockMultipartFile file1 = new MockMultipartFile("files", "photo1.jpg", "image/jpeg", JPEG_BYTES);
        MockMultipartFile file2 = new MockMultipartFile("files", "photo2.jpg", "image/jpeg", JPEG_BYTES);
        MockMultipartFile fakeFile = new MockMultipartFile(
                "files", "fake.jpg", "image/jpeg", "this is not an image".getBytes());

        mockMvc.perform(multipart("/api/stadiums/{stadiumId}/images", stadium.getId())
                        .file(file1).file(file2).file(fakeFile))
                .andExpect(status().isBadRequest());

        assertFileCountEventually(filesBefore);
        assertEquals(0, stadiumImageRepository.countByStadiumId(stadium.getId()));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void threeValidFiles_uploadSucceedsAndKeepsAllFilesAndRows() throws Exception {
        User owner = createOwner(OWNER_EMAIL);
        Stadium stadium = createStadium(owner, "Success Stadium");

        long filesBefore = countFilesOnDisk();

        MockMultipartFile file1 = new MockMultipartFile("files", "photo1.jpg", "image/jpeg", JPEG_BYTES);
        MockMultipartFile file2 = new MockMultipartFile("files", "photo2.jpg", "image/jpeg", JPEG_BYTES);
        MockMultipartFile file3 = new MockMultipartFile("files", "photo3.jpg", "image/jpeg", JPEG_BYTES);

        mockMvc.perform(multipart("/api/stadiums/{stadiumId}/images", stadium.getId())
                        .file(file1).file(file2).file(file3))
                .andExpect(status().isCreated());

        assertEquals(filesBefore + 3, countFilesOnDisk());
        List<StadiumImage> images = stadiumImageRepository.findByStadiumId(stadium.getId());
        assertEquals(3, images.size());
        images.forEach(img -> storedUrls.add(img.getImageUrl()));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void deletingImage_removesBothDbRowAndPhysicalFile() throws Exception {
        User owner = createOwner(OWNER_EMAIL);
        Stadium stadium = createStadium(owner, "Delete Stadium");
        StadiumImage image = createImageDirectly(stadium, true);
        Path filePath = resolveFile(image.getImageUrl());
        assertTrue(Files.exists(filePath));

        mockMvc.perform(delete("/api/stadiums/{stadiumId}/images/{imageId}", stadium.getId(), image.getId()))
                .andExpect(status().isNoContent());

        assertTrue(stadiumImageRepository.findById(image.getId()).isEmpty());
        assertFileGoneEventually(filePath);
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void deletingPrimaryImage_promotesNextRemainingImageToPrimary() throws Exception {
        User owner = createOwner(OWNER_EMAIL);
        Stadium stadium = createStadium(owner, "Primary Promotion Stadium");
        StadiumImage primary = createImageDirectly(stadium, true);
        StadiumImage secondary = createImageDirectly(stadium, false);

        mockMvc.perform(delete("/api/stadiums/{stadiumId}/images/{imageId}", stadium.getId(), primary.getId()))
                .andExpect(status().isNoContent());

        StadiumImage reloaded = stadiumImageRepository.findById(secondary.getId()).orElseThrow();
        assertTrue(reloaded.getIsPrimary());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void deletingLastRemainingImage_leavesStadiumWithZeroImages() throws Exception {
        User owner = createOwner(OWNER_EMAIL);
        Stadium stadium = createStadium(owner, "Last Image Stadium");
        StadiumImage only = createImageDirectly(stadium, true);

        mockMvc.perform(delete("/api/stadiums/{stadiumId}/images/{imageId}", stadium.getId(), only.getId()))
                .andExpect(status().isNoContent());

        assertTrue(stadiumImageRepository.findByStadiumId(stadium.getId()).isEmpty());
    }
}
