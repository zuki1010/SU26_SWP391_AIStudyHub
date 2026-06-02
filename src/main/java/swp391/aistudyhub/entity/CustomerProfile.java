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
@Table(name = "customer_profiles")
public class CustomerProfile {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "customer_id", nullable = false)
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

    @Size(max = 50)
    @Nationalized
    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "school_name")
    private String schoolName;


}