package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.response.NotificationResponse;
import swp391.aistudyhub.entity.Notification;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.NotificationType;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.NotificationRepository;
import swp391.aistudyhub.service.NotificationService;

import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void notify(User recipient, User actor, NotificationType type, String message, UUID targetId) {
        if (recipient == null) {
            return;
        }
        // Don't notify yourself about your own action.
        if (actor != null && Objects.equals(recipient.getId(), actor.getId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setMessage(message);
        notification.setTargetId(targetId);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipient_IdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AuthException("Không tìm thấy thông báo.", HttpStatus.NOT_FOUND));

        if (!Objects.equals(notification.getRecipient().getId(), userId)) {
            throw new AuthException("Bạn không có quyền với thông báo này.", HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse mapToResponse(Notification n) {
        String actorName = null;
        if (n.getActor() != null) {
            actorName = n.getActor().getCustomerProfile() != null
                    && n.getActor().getCustomerProfile().getFullName() != null
                    ? n.getActor().getCustomerProfile().getFullName()
                    : n.getActor().getEmail();
        }
        return NotificationResponse.builder()
                .notificationId(n.getId())
                .type(n.getType().name())
                .message(n.getMessage())
                .actorId(n.getActor() != null ? n.getActor().getId() : null)
                .actorName(actorName)
                .targetId(n.getTargetId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
