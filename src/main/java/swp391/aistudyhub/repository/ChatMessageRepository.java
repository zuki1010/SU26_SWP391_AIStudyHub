package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.ChatRequestResponse;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.ChatSession;
import swp391.aistudyhub.enums.SenderType;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop10ByChatSessionOrderBySentAtDesc(ChatSession chatSession);

    Page<ChatMessage> findByChatSessionOrderBySentAtDesc(ChatSession chatSession, Pageable pageable);

    Page<ChatMessage> findBySenderType(SenderType senderType, Pageable pageable);

    Page<ChatRequestResponse> findBySenderType(Pageable pageable, SenderType type);
}