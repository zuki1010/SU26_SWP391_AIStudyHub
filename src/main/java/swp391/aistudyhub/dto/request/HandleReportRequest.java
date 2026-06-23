package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import swp391.aistudyhub.enums.ReportStatus;

@Getter
@Setter
public class HandleReportRequest {

    /**
     * RESOLVED or DISMISSED.
     */
    @NotNull
    private ReportStatus status;

    /**
     * When true, the reported post is hidden / the reported comment is deleted.
     */
    private boolean hideContent = false;
}
