package org.example.sportstadiumbookingsystem.entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.sportstadiumbookingsystem.entityEnums.DayOfWeek;

import java.time.LocalTime;

@Entity
@Table(
        name = "working_hours",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_working_hours_stadium_day",
                        columnNames = {"stadium_id", "day_of_week"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // كل WorkingHour تابع لملعب واحد
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;
}
