package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import swp391.aistudyhub.enums.TargetType;

import java.util.UUID;

@Getter
@Setter
public class CreateReportRequest {

    @NotNull
    private TargetType targetType;

    @NotNull
    private UUID targetId;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
