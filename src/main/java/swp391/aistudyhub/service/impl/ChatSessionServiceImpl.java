package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.repository.ChatSessionRepository;
import swp391.aistudyhub.service.ChatSessionService;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

}
