package org.example.sportstadiumbookingsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.timeslot.GenerateTimeSlotsRangeRequest;
import org.example.sportstadiumbookingsystem.dto.timeslot.GenerateTimeSlotsRequest;
import org.example.sportstadiumbookingsystem.dto.timeslot.TimeSlotResponse;
import org.example.sportstadiumbookingsystem.service.TimeSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stadiums/{stadiumId}/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<TimeSlotResponse>> generateTimeSlots(
            @PathVariable Long stadiumId,
            @Valid @RequestBody GenerateTimeSlotsRequest request) {

        List<TimeSlotResponse> response = timeSlotService.generateTimeSlots(stadiumId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/generate-range")
    @PreAuthorize("hasRole('STADIUM_OWNER')")
    public ResponseEntity<List<TimeSlotResponse>> generateTimeSlotsRange(
            @PathVariable Long stadiumId,
            @Valid @RequestBody GenerateTimeSlotsRangeRequest request) {

        List<TimeSlotResponse> response = timeSlotService.generateTimeSlotsRange(stadiumId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimeSlotResponse>> getAvailableSlots(
            @PathVariable Long stadiumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TimeSlotResponse> response = timeSlotService.getAvailableSlots(stadiumId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('STADIUM_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<TimeSlotResponse>> getAllSlotsForDate(
            @PathVariable Long stadiumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TimeSlotResponse> response = timeSlotService.getAllSlotsForDate(stadiumId, date);
        return ResponseEntity.ok(response);
    }
}