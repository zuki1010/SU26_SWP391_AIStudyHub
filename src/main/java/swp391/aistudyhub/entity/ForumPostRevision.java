package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "forum_post_revisions")
public class ForumPostRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "revision_no")
    private UUID revisionNo = UUID.randomUUID();

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "status")
    private String status;

    @Column(name = "edited_by")
    private UUID editedBy;

    @Column(name = "edited_by_name")
    private String editedByName;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}
