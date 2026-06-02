package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.StartSessionDTO;

import java.util.UUID;

public interface ChatBotService {
    UUID createNewChatSession(StartSessionDTO dto);
}
