package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreatePostRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    private UUID categoryId;

    /**
     * Tag names; created on the fly if they don't exist yet.
     */
    private List<String> tags;

    /**
     * Ids of documents from the author's personal library to attach to this post.
     */
    private List<UUID> documentIds;
}
