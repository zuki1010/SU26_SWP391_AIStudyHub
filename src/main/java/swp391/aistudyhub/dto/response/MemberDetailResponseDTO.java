package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class MemberDetailResponseDTO {

    private String status;

    private String planName;

    private Instant startDate;

    private Instant endDate;
}
