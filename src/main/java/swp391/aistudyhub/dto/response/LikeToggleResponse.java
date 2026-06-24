package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeToggleResponse {
    private final boolean liked;
    private final long likeCount;
}
