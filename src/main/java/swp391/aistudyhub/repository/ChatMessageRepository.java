package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.dto.projection.ChatRequestResponse;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.ChatSession;
import swp391.aistudyhub.enums.SenderType;
import swp391.aistudyhub.enums.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop10ByChatSessionOrderBySentAtDesc(ChatSession chatSession);

    Page<ChatMessage> findByChatSessionOrderBySentAtDesc(ChatSession chatSession, Pageable pageable);

    Page<ChatRequestResponse> findBySenderType(Pageable pageable, SenderType type);
}