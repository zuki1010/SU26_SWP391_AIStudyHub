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

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_categories")
public class DocumentCategory {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "category_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Size(max = 256)
    @NotNull
    @Nationalized
    @Column(name = "category_name", nullable = false, length = 256)
    private String categoryName;

    @Size(max = 100)
    @Nationalized
    @Column(name = "category_type", length = 100)
    private String categoryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DocumentCategory parent;

    @NotNull
    @ColumnDefault("sysdatetimeoffset()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}