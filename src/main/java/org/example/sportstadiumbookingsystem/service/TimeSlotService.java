package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.timeslot.GenerateTimeSlotsRangeRequest;
import org.example.sportstadiumbookingsystem.dto.timeslot.GenerateTimeSlotsRequest;
import org.example.sportstadiumbookingsystem.dto.timeslot.TimeSlotResponse;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.TimeSlot;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entity.WorkingHour;
import org.example.sportstadiumbookingsystem.entityEnums.SlotStatus;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.TimeSlotRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.repository.WorkingHourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final StadiumRepository stadiumRepository;
    private final WorkingHourRepository workingHourRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<TimeSlotResponse> generateTimeSlots(Long stadiumId, GenerateTimeSlotsRequest request) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        checkOwnership(stadium);

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot generate slots for a past date");
        }

        List<TimeSlot> created = generateForDate(stadium, request.getDate(), request.getSlotDurationMinutes());
        return created.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<TimeSlotResponse> generateTimeSlotsRange(Long stadiumId, GenerateTimeSlotsRangeRequest request) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        checkOwnership(stadium);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot generate slots for a past date");
        }
        if (request.getStartDate().plusDays(90).isBefore(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range cannot exceed 90 days");
        }

        List<TimeSlot> allCreated = new ArrayList<>();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            allCreated.addAll(generateForDate(stadium, date, request.getSlotDurationMinutes()));
        }
        return allCreated.stream().map(this::toResponse).toList();
    }

    public List<TimeSlotResponse> getAvailableSlots(Long stadiumId, LocalDate date) {
        findStadiumOrThrow(stadiumId);

        List<TimeSlot> slots = timeSlotRepository
                .findByStadiumIdAndSlotDateAndStatusOrderByStartTimeAsc(stadiumId, date, SlotStatus.AVAILABLE);

        boolean isToday = date.isEqual(LocalDate.now());
        LocalTime now = LocalTime.now();

        return slots.stream()
                .filter(slot -> !isToday || slot.getStartTime().isAfter(now))
                .map(this::toResponse)
                .toList();
    }

    public List<TimeSlotResponse> getAllSlotsForDate(Long stadiumId, LocalDate date) {
        findStadiumOrThrow(stadiumId);
        return timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadiumId, date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Helper methods ────────────────────────────────────────────

    private List<TimeSlot> generateForDate(Stadium stadium, LocalDate date, Integer slotDurationMinutes) {
        int duration = (slotDurationMinutes == null || slotDurationMinutes <= 0) ? 60 : slotDurationMinutes;

        org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek dayOfWeek =
                mapToEntityDayOfWeek(date.getDayOfWeek());

        WorkingHour workingHour = workingHourRepository
                .findByStadiumIdAndDayOfWeek(stadium.getId(), dayOfWeek)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Working hours are not set for " + dayOfWeek + ". Please set working hours first."));

        if (Boolean.TRUE.equals(workingHour.getIsClosed())) {
            return List.of();
        }

        LocalTime openTime = workingHour.getOpenTime();
        LocalTime closeTime = workingHour.getCloseTime();

        if (openTime == null || closeTime == null || !closeTime.isAfter(openTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid working hours for " + dayOfWeek);
        }

        Set<LocalTime> existingStartTimes = new HashSet<>();
        timeSlotRepository.findByStadiumIdAndSlotDateOrderByStartTimeAsc(stadium.getId(), date)
                .forEach(slot -> existingStartTimes.add(slot.getStartTime()));

        BigDecimal pricePerSlot = stadium.getPricePerHour()
                .multiply(BigDecimal.valueOf(duration))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        List<TimeSlot> toCreate = new ArrayList<>();
        LocalTime cursor = openTime;

        while (!cursor.plusMinutes(duration).isAfter(closeTime) && cursor.plusMinutes(duration).isAfter(cursor)) {
            LocalTime slotEnd = cursor.plusMinutes(duration);

            if (!existingStartTimes.contains(cursor)) {
                toCreate.add(TimeSlot.builder()
                        .stadium(stadium)
                        .slotDate(date)
                        .startTime(cursor)
                        .endTime(slotEnd)
                        .status(SlotStatus.AVAILABLE)
                        .price(pricePerSlot)
                        .build());
            }
            cursor = slotEnd;
        }

        return toCreate.isEmpty() ? List.of() : timeSlotRepository.saveAll(toCreate);
    }

    private org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek mapToEntityDayOfWeek(java.time.DayOfWeek javaDay) {
        return org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek.valueOf(javaDay.name());
    }

    private void checkOwnership(Stadium stadium) {
        User currentUser = getCurrentUser();
        if (!stadium.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this stadium");
        }
    }

    private Stadium findStadiumOrThrow(Long stadiumId) {
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stadium not found"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private TimeSlotResponse toResponse(TimeSlot slot) {
        return TimeSlotResponse.builder()
                .id(slot.getId())
                .stadiumId(slot.getStadium().getId())
                .stadiumName(slot.getStadium().getName())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .price(slot.getPrice())
                .build();
    }
}