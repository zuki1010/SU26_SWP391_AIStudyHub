package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.UserSession;

import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
}