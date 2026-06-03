package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.ChatSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop10ByChatSessionOrderBySentAtDesc(ChatSession chatSession);

}