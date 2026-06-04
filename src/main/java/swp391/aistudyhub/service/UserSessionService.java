package swp391.aistudyhub.service;

import swp391.aistudyhub.entity.UserSession;

import java.util.Optional;

public interface UserSessionService {

    Optional<UserSession> findByRefreshToken(String refreshToken);

    void revokeByRefreshToken(String refreshToken);

    void revokeAllForUser(java.util.UUID userId);
}
