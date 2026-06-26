package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "document_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Size(max = 50)
    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "preview_url", columnDefinition = "text")
    private String previewUrl;

    @Column(name = "download_url", columnDefinition = "text")
    private String downloadUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_public", nullable = false, columnDefinition = "boolean default false")
    private boolean isPublic = false;

    @NotNull
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentCategory> documentCategories = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> documentChunks = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentVersion> documentVersions = new ArrayList<>();

    @Column(name = "description", columnDefinition = "TEXT")
private String description;
}