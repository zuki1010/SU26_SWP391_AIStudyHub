package swp391.aistudyhub.dto.request;

import lombok.Data;
import swp391.aistudyhub.enums.RequestPublicDoc;

import java.util.UUID;

@Data
public class ApprovePublicRequestDTO {
    UUID documentId;

    RequestPublicDoc rqd;
}
