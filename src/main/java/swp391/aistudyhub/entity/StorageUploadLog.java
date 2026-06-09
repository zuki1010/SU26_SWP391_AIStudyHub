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
@Table(name = "storage_upload_logs")
public class StorageUploadLog {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "log_id", nullable = false)
    private UUID id;

    @Size(max = 255)
    @NotNull
    @Nationalized
    @Column(name = "file_name_origin", nullable = false)
    private String fileNameOrigin;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Size(max = 50)
    @NotNull
    @Nationalized
    @Column(name = "upload_status", nullable = false, length = 50)
    private String uploadStatus;

    @ColumnDefault("getdate()")
    @Column(name = "uploaded_at")
    private Instant uploadedAt;


}