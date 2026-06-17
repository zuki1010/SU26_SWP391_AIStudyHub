package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_shares")
public class DocumentShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "share_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWithUser;

    @Size(max = 50)
    @NotNull
    @Column(name = "permission_type", nullable = false, length = 50)
    private String permissionType = "view";

    @Column(name = "shared_at", updatable = false)
    private Instant sharedAt = Instant.now();
}