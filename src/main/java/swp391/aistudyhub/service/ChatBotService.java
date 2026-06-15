package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.ChatMessageDTO;
import swp391.aistudyhub.dto.response.UpdateSessionDocsDTO;
import swp391.aistudyhub.entity.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface ChatBotService {
    UUID createNewChatSession(StartSessionDTO dto);

    void updateSessionDocuments(UUID sessionId, UpdateSessionDocsDTO dto);

    String chatWithGemini(ChatRequestSessionDTO dto);

    List<ChatMessageDTO> getChatHistory(UUID sessionId, int page, int size);
}
