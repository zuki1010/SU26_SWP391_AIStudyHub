package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Size(max = 50)
    @NotNull

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;

    @Column(name = "download_url", columnDefinition = "TEXT")
    private String downloadUrl;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}