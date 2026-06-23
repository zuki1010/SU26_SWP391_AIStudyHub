package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateCommentRequest {

    @NotBlank
    private String content;

    /**
     * Root comment id when this is a reply. Null for a top-level comment.
     * Replies are flattened to 2 levels: a reply to a reply still attaches to the root comment.
     */
    private UUID parentId;

    /**
     * Optional id of the comment being quoted.
     */
    private UUID quotedCommentId;
}
