package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "forum_tags")
public class ForumTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tag_id", updatable = false, nullable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;
}
