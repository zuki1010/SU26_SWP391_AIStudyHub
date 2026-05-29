package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "user_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "email", nullable = false)
    private String email;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'CUSTOMER'")
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Size(max = 50)
    @Nationalized
    @ColumnDefault("'ACTIVE'")
    @Column(name = "account_status", length = 50)
    private String accountStatus;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;


}