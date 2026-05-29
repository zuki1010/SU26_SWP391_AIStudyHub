package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "session_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Nationalized
    @Lob
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Size(max = 255)
    @Nationalized
    @Column(name = "device_info")
    private String deviceInfo;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;


}