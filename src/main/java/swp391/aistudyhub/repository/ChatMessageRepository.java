package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.ChatMessage;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
}