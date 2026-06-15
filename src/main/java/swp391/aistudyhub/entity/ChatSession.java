package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @ColumnDefault("newid()")
    @Column(name = "chat_session_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "session_documents",
            joinColumns = @JoinColumn(name = "chat_session_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
    private Set<Document> documents = new HashSet<>();

    @Size(max = 255)
    @Nationalized
    @Column(name = "session_title")
    private String sessionTitle;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;


    public void addDocument(Document doc) {
        this.documents.add(doc);
    }

    public void removeDocument(Document doc) {
        this.documents.remove(doc);
    }

}