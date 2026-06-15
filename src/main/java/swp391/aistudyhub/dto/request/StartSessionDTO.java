package swp391.aistudyhub.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StartSessionDTO {
    private List<UUID> documentIds;
}
