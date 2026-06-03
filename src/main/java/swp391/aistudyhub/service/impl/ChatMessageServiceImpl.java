package swp391.aistudyhub.service.impl;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.dto.ChatRequestSessionDTO;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.ChatSession;
import swp391.aistudyhub.repository.ChatMessageRepository;
import swp391.aistudyhub.repository.ChatSessionRepository;
import swp391.aistudyhub.service.ChatMessageService;
import swp391.aistudyhub.service.ChatSessionService;

import java.time.Instant;
import java.util.*;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

}
