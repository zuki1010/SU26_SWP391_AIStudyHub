package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chunk_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @NotNull
    @Column(name = "chunk_content", nullable = false, columnDefinition = "text")
    private String chunkContent;

    @Column(name = "vector_embedding", columnDefinition = "vector(1536)")
    private String vectorEmbedding;

    @Column(name = "page_number")
    private Integer pageNumber;
}