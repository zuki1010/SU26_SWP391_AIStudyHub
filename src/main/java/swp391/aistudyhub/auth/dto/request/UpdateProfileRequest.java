package swp391.aistudyhub.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 50)
    private String studentCode;

    @Size(max = 255)
    private String schoolName;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String assignedSubject;
}
