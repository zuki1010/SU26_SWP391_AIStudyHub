package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "admin_profiles")
public class AdminProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "admin_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "access_level")
    private Integer accessLevel = 1;

    @Column(name = "security_key", columnDefinition = "text")
    private String securityKey;
}