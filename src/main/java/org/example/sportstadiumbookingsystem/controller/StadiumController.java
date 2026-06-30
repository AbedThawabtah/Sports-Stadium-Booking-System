package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumRequest;
import org.example.sportstadiumbookingsystem.dto.stadium.StadiumResponse;
import org.example.sportstadiumbookingsystem.service.StadiumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<StadiumResponse>> getAllActiveStadiums() {
        List<StadiumResponse> stadiums = stadiumService.getAllActiveStadiums();
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