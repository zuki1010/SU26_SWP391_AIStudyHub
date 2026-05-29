package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.ChatSession;

import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
}