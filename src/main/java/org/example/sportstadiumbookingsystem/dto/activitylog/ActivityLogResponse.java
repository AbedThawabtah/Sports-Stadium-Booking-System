package org.example.sportstadiumbookingsystem.dto.activitylog;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ActivityLogResponse {

    private Long id;

    private Long userId;
    private String userName;

    private String eventType;
    private String entityType;
    private Long entityId;
    private String description;
    private String ipAddress;

    private LocalDateTime createdAt;
}