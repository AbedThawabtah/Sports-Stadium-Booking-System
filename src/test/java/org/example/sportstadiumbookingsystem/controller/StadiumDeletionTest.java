package org.example.sportstadiumbookingsystem.controller;

import org.example.sportstadiumbookingsystem.entity.Reservation;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.StadiumImage;
import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entity.WorkingHour;
import org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek;
import org.example.sportstadiumbookingsystem.entityEnums.ReservationStatus;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.ReservationRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumImageRepository;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.TimeSlotRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.repository.WorkingHourRepository;
import org.example.sportstadiumbookingsystem.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StadiumDeletionTest {

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
    private ReservationRepository reservationRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private WorkingHourRepository workingHourRepository;

    @Autowired
    private StadiumImageRepository stadiumImageRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> storedUrls = new ArrayList<>();

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
    }

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .fullName(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password"))
                .role(role)
                .isActive(true)
                .build());
    }

    private Stadium createStadium(User owner, String name) {
        return stadiumRepository.save(Stadium.builder()
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
    }

    private TimeSlot createTimeSlot(Stadium stadium) {
        return timeSlotRepository.save(TimeSlot.builder()
                .stadium(stadium)
                .slotDate(LocalDate.now().plusDays(10))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .status(SlotStatus.AVAILABLE)
                .price(new BigDecimal("50.00"))
                .build());
    }

    private void createWorkingHour(Stadium stadium) {
        workingHourRepository.save(WorkingHour.builder()
                .stadium(stadium)
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .isClosed(false)
                .build());
    }

    private StadiumImage createImage(Stadium stadium) {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_BYTES);
        String url = fileStorageService.store(file);
        storedUrls.add(url);
        return stadiumImageRepository.save(StadiumImage.builder()
                .stadium(stadium)
                .imageUrl(url)
                .isPrimary(true)
                .build());
    }

    private void createReservation(Stadium stadium, User customer, TimeSlot slot) {
        reservationRepository.save(Reservation.builder()
                .customer(customer)
                .timeSlot(slot)
                .stadium(stadium)
                .status(ReservationStatus.CANCELLED)
                .totalPrice(new BigDecimal("50.00"))
                .reservationDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .build());
    }

    @Test
    @WithMockUser(username = "owner-empty@test.com", roles = "STADIUM_OWNER")
    void ownerCanDeleteStadiumWithNoDependents() throws Exception {
        User owner = createUser("owner-empty@test.com", UserRole.STADIUM_OWNER);
        Stadium stadium = createStadium(owner, "Empty Stadium");

        mockMvc.perform(delete("/api/stadiums/{id}", stadium.getId()))
                .andExpect(status().isNoContent());

        assertTrue(stadiumRepository.findById(stadium.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "owner-withres@test.com", roles = "STADIUM_OWNER")
    void stadiumWithReservation_cannotBeDeleted() throws Exception {
        User owner = createUser("owner-withres@test.com", UserRole.STADIUM_OWNER);
        User customer = createUser("customer-withres@test.com", UserRole.CUSTOMER);
        Stadium stadium = createStadium(owner, "Reserved Stadium");
        TimeSlot slot = createTimeSlot(stadium);
        createWorkingHour(stadium);
        createImage(stadium);
        createReservation(stadium, customer, slot);

        mockMvc.perform(delete("/api/stadiums/{id}", stadium.getId()))
                .andExpect(status().isConflict());

        assertTrue(stadiumRepository.findById(stadium.getId()).isPresent());
        assertEquals(1, reservationRepository.countByStadiumId(stadium.getId()));
        assertEquals(1, timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(
                stadium.getId(), slot.getSlotDate()).size());
        assertEquals(1, workingHourRepository.findByStadiumIdOrderByDayOfWeekAsc(stadium.getId()).size());
        assertEquals(1, stadiumImageRepository.findByStadiumId(stadium.getId()).size());
    }

    @Test
    @WithMockUser(username = "owner-dependents@test.com", roles = "STADIUM_OWNER")
    void stadiumWithDependentsButNoReservations_canBeDeletedAndDependentsRemoved() throws Exception {
        User owner = createUser("owner-dependents@test.com", UserRole.STADIUM_OWNER);
        Stadium stadium = createStadium(owner, "Dependents Stadium");
        createTimeSlot(stadium);
        createWorkingHour(stadium);
        createImage(stadium);

        mockMvc.perform(delete("/api/stadiums/{id}", stadium.getId()))
                .andExpect(status().isNoContent());

        assertTrue(stadiumRepository.findById(stadium.getId()).isEmpty());
        assertTrue(timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(
                stadium.getId(), LocalDate.now().plusDays(10)).isEmpty());
        assertTrue(workingHourRepository.findByStadiumIdOrderByDayOfWeekAsc(stadium.getId()).isEmpty());
        assertTrue(stadiumImageRepository.findByStadiumId(stadium.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "owner-two@test.com", roles = "STADIUM_OWNER")
    void ownerCannotDeleteAnotherOwnersStadium() throws Exception {
        User ownerOne = createUser("owner-one@test.com", UserRole.STADIUM_OWNER);
        createUser("owner-two@test.com", UserRole.STADIUM_OWNER);
        Stadium stadium = createStadium(ownerOne, "Not Yours Stadium");

        mockMvc.perform(delete("/api/stadiums/{id}", stadium.getId()))
                .andExpect(status().isForbidden());

        assertTrue(stadiumRepository.findById(stadium.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void adminCanDeleteAnyStadiumWithNoReservations() throws Exception {
        User owner = createUser("owner-for-admin@test.com", UserRole.STADIUM_OWNER);
        createUser("admin@test.com", UserRole.ADMIN);
        Stadium stadium = createStadium(owner, "Admin Deletable Stadium");
        createWorkingHour(stadium);

        mockMvc.perform(delete("/api/stadiums/{id}", stadium.getId()))
                .andExpect(status().isNoContent());

        assertTrue(stadiumRepository.findById(stadium.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "owner-missingfile@test.com", roles = "STADIUM_OWNER")
    void deletingStadiumWithImages_doesNotThrowWhenPhysicalFileAlreadyMissing() throws Exception {
        User owner = createUser("owner-missingfile@test.com", UserRole.STADIUM_OWNER);
        Stadium stadium = createStadium(owner, "Missing File Stadium");
        StadiumImage image = createImage(stadium);

        // Simulate the physical file already being gone from disk before deletion runs.
        fileStorageService.delete(image.getImageUrl());

        mockMvc.perform(delete("/api/stadiums/{id}", stadium.getId()))
                .andExpect(status().isNoContent());

        assertTrue(stadiumRepository.findById(stadium.getId()).isEmpty());
        assertTrue(stadiumImageRepository.findByStadiumId(stadium.getId()).isEmpty());
    }
}
