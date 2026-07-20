package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDetailResponseDTO {
    private UUID userId;
    private String userFullName;
    private String userEmail;
}
