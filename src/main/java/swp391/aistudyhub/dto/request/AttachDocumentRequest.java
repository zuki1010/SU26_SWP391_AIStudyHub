package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AttachDocumentRequest {

    /**
     * Ids of documents from the requester's personal library to attach.
     */
    @NotEmpty
    private List<UUID> documentIds;

    /**
     * Optional: attach to a specific comment instead of the post body.
     */
    private UUID commentId;
}
