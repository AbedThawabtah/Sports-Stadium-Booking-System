package org.example.sportstadiumbookingsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.workinghour.WorkingHourRequest;
import org.example.sportstadiumbookingsystem.dto.workinghour.WorkingHourResponse;
import org.example.sportstadiumbookingsystem.entity.Stadium;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.entity.WorkingHour;
import org.example.sportstadiumbookingsystem.repository.StadiumRepository;
import org.example.sportstadiumbookingsystem.repository.UserRepository;
import org.example.sportstadiumbookingsystem.repository.WorkingHourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkingHourService {

    private final WorkingHourRepository workingHourRepository;
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final StadiumService stadiumService;

    @Transactional
    public List<WorkingHourResponse> setWorkingHours(Long stadiumId, List<WorkingHourRequest> requests) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        User currentUser = getCurrentUser();

        if (!stadium.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this stadium");
        }

        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Working hours list cannot be empty");
        }

        // ما بنسمح بتكرار نفس اليوم أكثر من مرة بنفس الطلب
        Set<org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek> seenDays = new HashSet<>();

        for (WorkingHourRequest req : requests) {
            if (!seenDays.add(req.getDayOfWeek())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate day in request: " + req.getDayOfWeek());
            }

            boolean isClosed = Boolean.TRUE.equals(req.getIsClosed());
            if (!isClosed) {
                if (req.getOpenTime() == null || req.getCloseTime() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "openTime and closeTime are required when the day is not closed (" + req.getDayOfWeek() + ")");
                }
                if (!req.getCloseTime().isAfter(req.getOpenTime())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "closeTime must be after openTime (" + req.getDayOfWeek() + ")");
                }
            }
        }

        // استبدال كامل: نحذف القديم ونضيف الجديد
        workingHourRepository.deleteByStadiumId(stadiumId);
        workingHourRepository.flush();

        List<WorkingHour> entities = requests.stream()
                .map(req -> WorkingHour.builder()
                        .stadium(stadium)
                        .dayOfWeek(req.getDayOfWeek())
                        .openTime(req.getOpenTime())
                        .closeTime(req.getCloseTime())
                        .isClosed(Boolean.TRUE.equals(req.getIsClosed()))
                        .build())
                .toList();

        List<WorkingHour> saved = workingHourRepository.saveAll(entities);

        return saved.stream()
                .map(this::toResponse)
                .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .toList();
    }

    public List<WorkingHourResponse> getWorkingHours(Long stadiumId) {
        Stadium stadium = findStadiumOrThrow(stadiumId);
        stadiumService.assertStadiumViewable(stadium);
        return workingHourRepository.findByStadiumIdOrderByDayOfWeekAsc(stadiumId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Helper methods ────────────────────────────────────────────

    private Stadium findStadiumOrThrow(Long stadiumId) {
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stadium not found"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private WorkingHourResponse toResponse(WorkingHour wh) {
        return WorkingHourResponse.builder()
                .id(wh.getId())
                .dayOfWeek(wh.getDayOfWeek())
                .openTime(wh.getOpenTime())
                .closeTime(wh.getCloseTime())
                .isClosed(wh.getIsClosed())
                .build();
    }
}