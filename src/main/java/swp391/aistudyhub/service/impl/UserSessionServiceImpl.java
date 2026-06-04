package swp391.aistudyhub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.entity.UserSession;
import swp391.aistudyhub.repository.UserSessionRepository;
import swp391.aistudyhub.service.UserSessionService;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;

    @Override
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        return userSessionRepository.findByRefreshToken(refreshToken);
    }

    @Override
    public void revokeByRefreshToken(String refreshToken) {
        userSessionRepository.deleteByRefreshToken(refreshToken);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        userSessionRepository.deleteByUser_Id(userId);
    }
}
