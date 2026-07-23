package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chat_session_id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Size(max = 255)
    @Column(name = "session_title")
    private String sessionTitle;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages = new ArrayList<>();
}