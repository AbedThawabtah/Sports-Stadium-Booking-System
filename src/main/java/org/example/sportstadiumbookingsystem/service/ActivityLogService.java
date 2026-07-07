package org.example.sportstadiumbookingsystem.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sportstadiumbookingsystem.dto.activitylog.ActivityLogResponse;
import org.example.sportstadiumbookingsystem.entity.ActivityLog;
import org.example.sportstadiumbookingsystem.entity.User;
import org.example.sportstadiumbookingsystem.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    // بيستخدمها أي Service تاني لتسجيل حدث مهم (تسجيل دخول، إنشاء ملعب، إلغاء حجز...)
    public void log(User user, String eventType, String entityType, Long entityId, String description) {
        ActivityLog entry = ActivityLog.builder()
                .user(user)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(extractClientIp())
                .build();

        activityLogRepository.save(entry);
    }

    public List<ActivityLogResponse> getAllLogs() {
        return activityLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ActivityLogResponse> getLogsForUser(Long userId) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Helper methods ────────────────────────────────────────────

    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            // ما بدنا نفشل أي عملية بس لأنه ما قدرنا نجيب الـ IP
            return null;
        }
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getFullName() : "System")
                .eventType(log.getEventType())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}