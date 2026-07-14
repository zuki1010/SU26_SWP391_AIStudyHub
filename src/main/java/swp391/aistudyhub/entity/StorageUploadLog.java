package swp391.aistudyhub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "storage_upload_logs")
public class StorageUploadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "storage_id", nullable = false)
    private CloudStorage cloudStorage;

    @NotNull
    @Column(name = "file_name_origin", nullable = false, columnDefinition = "text")
    private String fileNameOrigin;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Size(max = 50)
    @NotNull
    @Column(name = "upload_status", nullable = false, length = 50)
    private String uploadStatus = "pending";

    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt = Instant.now();
}