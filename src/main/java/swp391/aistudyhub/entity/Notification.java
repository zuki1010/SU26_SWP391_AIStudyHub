package swp391.aistudyhub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import swp391.aistudyhub.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * A notification delivered to a recipient (e.g. someone commented on their post,
 * or they were @mentioned). Generic enough to be reused beyond the forum.
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * The user who triggered the notification (commenter, mentioner...). May be null for system events.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "actor_id")
    private User actor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Size(max = 500)
    @Column(name = "message", length = 500)
    private String message;

    /**
     * Id of the related post or comment so the client can deep-link to it.
     */
    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
