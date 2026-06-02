package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    @ColumnDefault("newid()")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @NotNull
    @Nationalized
    @Lob
    @Column(name = "chunk_content", nullable = false)
    private String chunkContent;

    @Nationalized
    @Lob
    @Column(name = "vector_embedding")
    private String vectorEmbedding;

    @Column(name = "page_number")
    private Integer pageNumber;


}