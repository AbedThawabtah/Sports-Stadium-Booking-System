package org.example.sportstadiumbookingsystem.controller;

import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entityEnums.StadiumStatus;
import org.example.sportstadiumbookingsystem.entityEnums.UserRole;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StadiumVisibilityTest {

    private static final String OWNER_EMAIL = "vis-owner@test.com";
    private static final String OTHER_OWNER_EMAIL = "vis-other-owner@test.com";
    private static final String ADMIN_EMAIL = "vis-admin@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Stadium activeStadium;
    private Stadium pendingStadium;
    private Stadium suspendedStadium;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .fullName("Visibility Owner")
                .email(OWNER_EMAIL)
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Other Owner")
                .email(OTHER_OWNER_EMAIL)
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Admin")
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.ADMIN)
                .isActive(true)
                .build());

        activeStadium = stadiumRepository.save(baseStadium(owner, "Active Stadium", StadiumStatus.ACTIVE));
        pendingStadium = stadiumRepository.save(baseStadium(owner, "Pending Stadium", StadiumStatus.PENDING_APPROVAL));
        suspendedStadium = stadiumRepository.save(baseStadium(owner, "Suspended Stadium", StadiumStatus.SUSPENDED));
    }

    private Stadium baseStadium(User owner, String name, StadiumStatus status) {
        return Stadium.builder()
                .owner(owner)
                .name(name)
                .location("Loc")
                .city("Ramallah")
                .sportType("Football")
                .pricePerHour(new BigDecimal("50.00"))
                .status(status)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build();
    }

    private void assertAllEndpoints(Long stadiumId, org.springframework.test.web.servlet.request.RequestPostProcessor actor,
                                     ResultMatcher expected) throws Exception {
        LocalDate date = LocalDate.now().plusDays(30);

        perform(get("/api/stadiums/{id}", stadiumId), actor).andExpect(expected);
        perform(get("/api/stadiums/{stadiumId}/working-hours", stadiumId), actor).andExpect(expected);
        perform(get("/api/stadiums/{stadiumId}/time-slots", stadiumId).param("date", date.toString()), actor).andExpect(expected);
        perform(get("/api/stadiums/{stadiumId}/reviews", stadiumId), actor).andExpect(expected);
        perform(get("/api/stadiums/{stadiumId}/images", stadiumId), actor).andExpect(expected);
    }

    private org.springframework.test.web.servlet.ResultActions perform(MockHttpServletRequestBuilder builder,
                                                                         org.springframework.test.web.servlet.request.RequestPostProcessor actor) throws Exception {
        if (actor != null) {
            builder = builder.with(actor);
        }
        return mockMvc.perform(builder);
    }

    @Test
    void anonymous_canViewActiveStadium() throws Exception {
        assertAllEndpoints(activeStadium.getId(), null, status().isOk());
    }

    @Test
    void anonymous_getsNotFoundForPendingStadium() throws Exception {
        assertAllEndpoints(pendingStadium.getId(), null, status().isNotFound());
    }

    @Test
    void anonymous_getsNotFoundForSuspendedStadium() throws Exception {
        assertAllEndpoints(suspendedStadium.getId(), null, status().isNotFound());
    }

    @Test
    void owner_canViewOwnPendingAndSuspendedStadium() throws Exception {
        var owner = SecurityMockMvcRequestPostProcessors.user(OWNER_EMAIL).roles("STADIUM_OWNER");
        assertAllEndpoints(pendingStadium.getId(), owner, status().isOk());
        assertAllEndpoints(suspendedStadium.getId(), owner, status().isOk());
    }

    @Test
    void differentOwner_getsNotFoundForAnotherOwnersNonActiveStadium() throws Exception {
        var otherOwner = SecurityMockMvcRequestPostProcessors.user(OTHER_OWNER_EMAIL).roles("STADIUM_OWNER");
        assertAllEndpoints(pendingStadium.getId(), otherOwner, status().isNotFound());
        assertAllEndpoints(suspendedStadium.getId(), otherOwner, status().isNotFound());
    }

    @Test
    void admin_canViewAnyStadiumRegardlessOfStatus() throws Exception {
        var admin = SecurityMockMvcRequestPostProcessors.user(ADMIN_EMAIL).roles("ADMIN");
        assertAllEndpoints(activeStadium.getId(), admin, status().isOk());
        assertAllEndpoints(pendingStadium.getId(), admin, status().isOk());
        assertAllEndpoints(suspendedStadium.getId(), admin, status().isOk());
    }
}
