package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateSessionDocsDTO {
    private List<UUID> documentIds;
}
