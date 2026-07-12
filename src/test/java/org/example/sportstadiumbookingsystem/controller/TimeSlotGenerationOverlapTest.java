package org.example.sportstadiumbookingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sportstadiumbookingsystem.dto.timeslot.GenerateTimeSlotsRequest;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entity.WorkingHour;
import org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.TimeSlotRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.repository.WorkingHourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimeSlotGenerationOverlapTest {

    private static final String OWNER_EMAIL = "owner-ts@test.com";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkingHourRepository workingHourRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Stadium stadium;
    private LocalDate openDate;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .fullName("TimeSlot Owner")
                .email(OWNER_EMAIL)
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());

        stadium = stadiumRepository.save(Stadium.builder()
                .owner(owner)
                .name("Overlap Test Stadium")
                .location("Loc")
                .city("Ramallah")
                .sportType("Football")
                .pricePerHour(new BigDecimal("60.00"))
                .status(StadiumStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        openDate = LocalDate.now().plusDays(30);
        setWorkingHours(openDate, LocalTime.of(9, 0), LocalTime.of(12, 0), false);
    }

    private void setWorkingHours(LocalDate date, LocalTime open, LocalTime close, boolean closed) {
        DayOfWeek day = DayOfWeek.valueOf(date.getDayOfWeek().name());
        workingHourRepository.save(WorkingHour.builder()
                .stadium(stadium)
                .dayOfWeek(day)
                .openTime(open)
                .closeTime(close)
                .isClosed(closed)
                .build());
    }

    private void generate(LocalDate date, int durationMinutes) throws Exception {
        GenerateTimeSlotsRequest request = new GenerateTimeSlotsRequest();
        request.setDate(date);
        request.setSlotDurationMinutes(durationMinutes);

        mockMvc.perform(post("/api/stadiums/{stadiumId}/time-slots/generate", stadium.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void regeneratingSameDuration_createsNoDuplicatesAndDoesNotThrow() throws Exception {
        generate(openDate, 60);
        generate(openDate, 60);

        List<TimeSlot> slots = timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), openDate);
        assertEquals(3, slots.size());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void adjacentSlots_areAllowedAndDoNotConflict() throws Exception {
        generate(openDate, 60);

        List<TimeSlot> slots = timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), openDate);
        assertEquals(3, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), slots.get(0).getEndTime());
        assertEquals(LocalTime.of(10, 0), slots.get(1).getStartTime());
        assertEquals(LocalTime.of(11, 0), slots.get(1).getEndTime());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void differentDurationOverlappingExistingSlot_returns409AndLeavesOriginalUnchanged() throws Exception {
        generate(openDate, 60);
        List<TimeSlot> before = timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), openDate);
        Long originalId = before.get(0).getId();

        GenerateTimeSlotsRequest request = new GenerateTimeSlotsRequest();
        request.setDate(openDate);
        request.setSlotDurationMinutes(30);

        mockMvc.perform(post("/api/stadiums/{stadiumId}/time-slots/generate", stadium.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        List<TimeSlot> after = timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), openDate);
        assertEquals(3, after.size());
        assertEquals(originalId, after.get(0).getId());
        assertEquals(LocalTime.of(9, 0), after.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), after.get(0).getEndTime());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void bookedSlot_isNeverModifiedByGeneration() throws Exception {
        generate(openDate, 60);
        List<TimeSlot> slots = timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), openDate);
        TimeSlot booked = slots.get(0);
        booked.setStatus(SlotStatus.BOOKED);
        timeSlotRepository.save(booked);

        // Idempotent regeneration must not touch the booked slot
        generate(openDate, 60);

        // Conflicting regeneration must be rejected and must not touch the booked slot either
        GenerateTimeSlotsRequest conflicting = new GenerateTimeSlotsRequest();
        conflicting.setDate(openDate);
        conflicting.setSlotDurationMinutes(30);
        mockMvc.perform(post("/api/stadiums/{stadiumId}/time-slots/generate", stadium.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflicting)))
                .andExpect(status().isConflict());

        TimeSlot reloaded = timeSlotRepository.findById(booked.getId()).orElseThrow();
        assertEquals(SlotStatus.BOOKED, reloaded.getStatus());
        assertEquals(LocalTime.of(9, 0), reloaded.getStartTime());
        assertEquals(LocalTime.of(10, 0), reloaded.getEndTime());
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "STADIUM_OWNER")
    void closedDay_generatesNothing() throws Exception {
        LocalDate closedDate = openDate.plusDays(1);
        setWorkingHours(closedDate, null, null, true);

        generate(closedDate, 60);

        List<TimeSlot> slots = timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), closedDate);
        assertTrue(slots.isEmpty());
    }
}
