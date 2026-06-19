package swp391.aistudyhub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cloud_storage")
public class CloudStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "storage_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Column(name = "total_quota", nullable = false)
    private Long totalQuota = 5368709120L;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "used_quota", nullable = false)
    private Long usedQuota = 0L;

    @OneToMany(mappedBy = "cloudStorage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StorageUploadLog> uploadLogs = new ArrayList<>();

    public List<Document> getDocuments() {
        if (this.user != null) {
            return this.user.getDocuments();
        }
        return new java.util.ArrayList<>();
    }
}