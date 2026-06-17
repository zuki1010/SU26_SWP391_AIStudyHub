package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_versions")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @NotNull
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @NotNull
    @Column(name = "file_secure_path", nullable = false, columnDefinition = "text")
    private String fileSecurePath;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}