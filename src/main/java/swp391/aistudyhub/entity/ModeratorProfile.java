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

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "moderator_profiles")
public class ModeratorProfile {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "moderator_id", nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Size(max = 100)
    @Nationalized
    @Column(name = "department", length = 100)
    private String department;

    @Size(max = 100)
    @Nationalized
    @Column(name = "assigned_subject", length = 100)
    private String assignedSubject;


}