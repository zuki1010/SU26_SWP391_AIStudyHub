package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.aistudyhub.dto.response.NotificationResponse;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.NotificationType;

import java.util.UUID;

public interface NotificationService {

    /**
     * Creates and persists a notification. No-op if recipient equals actor (don't notify yourself).
     */
    void notify(User recipient, User actor, NotificationType type, String message, UUID targetId);

    Page<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable);

    long countUnread(UUID userId);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);
}
