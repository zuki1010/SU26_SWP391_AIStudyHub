package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.UpdateSessionDocsDTO;

import java.util.UUID;

public interface ChatBotService {
    UUID createNewChatSession(StartSessionDTO dto);
    void updateSessionDocuments(UUID sessionId, UpdateSessionDocsDTO dto);
    public String chatTest(String message);
}
