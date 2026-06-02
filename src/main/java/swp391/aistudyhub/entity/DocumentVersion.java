package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "document_versions")
public class DocumentVersion {
    @Id
    @ColumnDefault("newid()")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @NotNull
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @NotNull
    @Nationalized
    @Lob
    @Column(name = "file_secure_path", nullable = false)
    private String fileSecurePath;

    @Nationalized
    @Lob
    @Column(name = "description")
    private String description;

    @ColumnDefault("getdate()")
    @Column(name = "updated_at")
    private Instant updatedAt;


}