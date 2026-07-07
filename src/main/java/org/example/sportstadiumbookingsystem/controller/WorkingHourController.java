package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.workinghour.WorkingHourRequest;
import org.example.sportstadiumbookingsystem.dto.workinghour.WorkingHourResponse;
import org.example.sportstadiumbookingsystem.service.WorkingHourService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stadiums/{stadiumId}/working-hours")
@RequiredArgsConstructor
@Validated
public class WorkingHourController {

    private final WorkingHourService workingHourService;

    @PutMapping
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<WorkingHourResponse>> setWorkingHours(
            @PathVariable Long stadiumId,
            @Valid @RequestBody @NotEmpty(message = "Working hours list cannot be empty")
            List<WorkingHourRequest> requests) {

        List<WorkingHourResponse> response = workingHourService.setWorkingHours(stadiumId, requests);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkingHourResponse>> getWorkingHours(@PathVariable Long stadiumId) {
        List<WorkingHourResponse> response = workingHourService.getWorkingHours(stadiumId);
        return ResponseEntity.ok(response);
    }
}