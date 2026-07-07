package org.example.sportstadiumbookingsystem.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sportstadiumbookingsystem.service.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCompletionScheduler {

    private final ReservationService reservationService;

    // كل 15 دقيقة، نحوّل الحجوزات اللي انتهى وقتها من CONFIRMED إلى COMPLETED
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void completePastReservations() {
        log.info("Running scheduled task: auto-completing past reservations");
        reservationService.autoCompletePastReservations();
    }
}