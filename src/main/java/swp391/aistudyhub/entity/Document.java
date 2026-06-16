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
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "document_id", nullable = false)
    private UUID id;

    // --- ĐÃ ĐỔI: Thay thế mối quan hệ từ User sang CloudStorage ---
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "storage_id", nullable = false)
    private CloudStorage storage;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Size(max = 50)
    @NotNull
    @Nationalized
    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Nationalized
    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;

    @Nationalized
    @Column(name = "download_url", columnDefinition = "TEXT")
    private String downloadUrl;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;
}