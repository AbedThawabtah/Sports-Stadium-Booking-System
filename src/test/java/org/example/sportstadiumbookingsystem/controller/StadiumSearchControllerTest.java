package org.example.sportstadiumbookingsystem.controller;

import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.TimeSlotRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StadiumSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .fullName("Stadium Owner")
                .email("search-owner@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());

        Stadium eliteArena = stadiumRepository.save(Stadium.builder()
                .owner(owner)
                .name("Elite Arena")
                .description("Premium football pitch")
                .location("Main Street")
                .city("Ramallah")
                .sportType("Football")
                .capacity(200)
                .pricePerHour(new BigDecimal("100.00"))
                .status(StadiumStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        stadiumRepository.save(Stadium.builder()
                .owner(owner)
                .name("City Court")
                .description("Basketball court downtown")
                .location("Center Street")
                .city("Nablus")
                .sportType("Basketball")
                .capacity(80)
                .pricePerHour(new BigDecimal("40.00"))
                .status(StadiumStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        // Not yet approved: must never appear in public search results
        stadiumRepository.save(Stadium.builder()
                .owner(owner)
                .name("Pending Field")
                .description("Awaiting admin approval")
                .location("Side Street")
                .city("Ramallah")
                .sportType("Football")
                .capacity(50)
                .pricePerHour(new BigDecimal("60.00"))
                .status(StadiumStatus.PENDING_APPROVAL)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        timeSlotRepository.save(TimeSlot.builder()
                .stadium(eliteArena)
                .slotDate(LocalDate.of(2026, 7, 10))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .status(SlotStatus.AVAILABLE)
                .price(new BigDecimal("100.00"))
                .build());
    }

    @Test
    void searchByCity_returnsOnlyMatchingActiveStadiums() throws Exception {
        mockMvc.perform(get("/api/stadiums").param("city", "Ramallah"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Elite Arena"));
    }

    @Test
    void searchBySportType_returnsOnlyMatchingStadiums() throws Exception {
        mockMvc.perform(get("/api/stadiums").param("sportType", "Basketball"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("City Court"));
    }

    @Test
    void searchByPriceRange_returnsStadiumsWithinRange() throws Exception {
        mockMvc.perform(get("/api/stadiums")
                        .param("minPrice", "50")
                        .param("maxPrice", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Elite Arena"));
    }

    @Test
    void searchByKeyword_matchesNameDescriptionLocationOrCity() throws Exception {
        mockMvc.perform(get("/api/stadiums").param("keyword", "downtown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("City Court"));
    }

    @Test
    void pagination_limitsResultsAndExposesMetadata() throws Exception {
        mockMvc.perform(get("/api/stadiums")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.size").value(1));
    }

    @Test
    void dateFilter_returnsOnlyStadiumsWithAvailableSlotOnThatDate() throws Exception {
        mockMvc.perform(get("/api/stadiums").param("date", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Elite Arena"));
    }

    @Test
    void pendingStadiums_areNeverReturnedInPublicSearch() throws Exception {
        mockMvc.perform(get("/api/stadiums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }
}
