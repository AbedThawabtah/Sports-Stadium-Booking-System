package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.stadium.MyStadiumResponse;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumRequest;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumResponse;
import org.example.sportstadiumbookingsystem.service.StadiumService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    @PostMapping
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<StadiumResponse> createStadium(@Valid @RequestBody StadiumRequest request) {
        StadiumResponse response = stadiumService.createStadium(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<StadiumResponse>> searchStadiums(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sportType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<StadiumResponse> stadiums = stadiumService.searchStadiums(
                keyword, city, sportType, minPrice, maxPrice, date, pageable);
        return ResponseEntity.ok(stadiums);
    }

    @GetMapping("/my-stadiums")
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<MyStadiumResponse>> getMyStadiums() {
        List<MyStadiumResponse> stadiums = stadiumService.getMyStadiums();
        return ResponseEntity.ok(stadiums);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StadiumResponse> getStadiumById(@PathVariable Long id) {
        StadiumResponse stadium = stadiumService.getStadiumById(id);
        return ResponseEntity.ok(stadium);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<StadiumResponse> updateStadium(@PathVariable Long id,
                                                         @Valid @RequestBody StadiumRequest request) {
        StadiumResponse response = stadiumService.updateStadium(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STADIUM_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStadium(@PathVariable Long id) {
        stadiumService.deleteStadium(id);
        return ResponseEntity.noContent().build();
    }
}