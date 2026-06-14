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
    @ColumnDefault("newid()")
    @Column(name = "document_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;


}