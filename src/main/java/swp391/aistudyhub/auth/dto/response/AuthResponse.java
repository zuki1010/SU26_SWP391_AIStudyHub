package swp391.aistudyhub.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresInMs;
    private final UserProfileResponse user;
}
