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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StadiumOwnerAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User ownerOne = userRepository.save(User.builder()
                .fullName("Owner One")
                .email("owner1@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());

        User ownerTwo = userRepository.save(User.builder()
                .fullName("Owner Two")
                .email("owner2@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(UserRole.STADIUM_OWNER)
                .isActive(true)
                .build());

        stadiumRepository.save(Stadium.builder()
                .owner(ownerOne)
                .name("Owner One Stadium")
                .location("Loc 1")
                .city("Ramallah")
                .sportType("Football")
                .pricePerHour(new BigDecimal("50.00"))
                .status(StadiumStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        stadiumRepository.save(Stadium.builder()
                .owner(ownerTwo)
                .name("Owner Two Stadium")
                .location("Loc 2")
                .city("Nablus")
                .sportType("Basketball")
                .pricePerHour(new BigDecimal("30.00"))
                .status(StadiumStatus.ACTIVE)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());
    }

    @Test
    @WithMockUser(username = "owner1@test.com", roles = "STADIUM_OWNER")
    void owner_seesOnlyOwnStadiums() throws Exception {
        mockMvc.perform(get("/api/stadiums/my-stadiums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Owner One Stadium"));
    }

    @Test
    @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
    void nonOwner_cannotAccessMyStadiums() throws Exception {
        mockMvc.perform(get("/api/stadiums/my-stadiums"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_cannotAccessMyStadiums() throws Exception {
        mockMvc.perform(get("/api/stadiums/my-stadiums"))
                .andExpect(status().isForbidden());
    }
}
