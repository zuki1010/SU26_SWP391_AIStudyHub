package swp391.aistudyhub.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Pattern(regexp = "CUSTOMER|ADMIN|MODERATOR", message = "role must be CUSTOMER, ADMIN, or MODERATOR")
    private String role = "CUSTOMER";

    @Size(max = 50)
    private String studentCode;

    @Size(max = 255)
    private String schoolName;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String assignedSubject;
}
