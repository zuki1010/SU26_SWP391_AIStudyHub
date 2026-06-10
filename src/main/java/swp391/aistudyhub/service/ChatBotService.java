package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.request.ChatRequestSessionDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;

import java.util.UUID;

public interface ChatBotService {
    UUID createNewChatSession(StartSessionDTO dto);
    String chatWithNoDocument(ChatRequestSessionDTO dto);
    public String chatTest(String message);
}
