package swp391.aistudyhub.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.response.AdminChatResponseDTO;
import swp391.aistudyhub.dto.response.AdminDocumentResponseDTO;
import swp391.aistudyhub.dto.response.AdminStorageResponseDTO;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.entity.ChatMessage;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.SenderType;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.repository.ChatMessageRepository;
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.AdminService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CloudStorageRepository cloudStorageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserAccountResponseDTO> getAllCustomer(String key, int page, int size) {
        Pageable pageable = PageRequest.of(
                safePage(page),
                safeSize(size),
                Sort.by("createdAt").descending()
        );

        Page<User> users;

        if (key != null && !key.trim().isEmpty()) {
            users = userRepository.searchUsers(key.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(this::mapUserAccount);
    }

    @Override
    @Transactional
    public UserAccountResponseDTO updateUserStatus(UUID id, AccountStatus status) {
        if (status == null) {
            throw new RuntimeException("Trạng thái tài khoản không hợp lệ!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("This user does not exist!"));

        user.setAccountStatus(status);

        User savedUser = userRepository.saveAndFlush(user);

        return mapUserAccount(savedUser);
    }

    @Override
    @Transactional
    public UserAccountResponseDTO updateUserRole(UUID id, UserRole role) {
        if (role == null) {
            throw new RuntimeException("Role không hợp lệ!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("This user does not exist!"));

        user.setRole(role);

        User savedUser = userRepository.saveAndFlush(user);

        return mapUserAccount(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDocumentResponseDTO> getAllDocument(
            String key,
            String status,
            Boolean isPublic,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                safePage(page),
                safeSize(size),
                Sort.by("createdAt").descending()
        );

        String cleanKey = key != null && !key.trim().isEmpty()
                ? key.trim()
                : null;

        StatusPublicDoc statusEnum = parseStatus(status);

        Page<Document> documents = documentRepository.searchAdminDocuments(
                cleanKey,
                statusEnum,
                isPublic,
                pageable
        );

        return documents.map(this::mapAdminDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChatResponseDTO> getAllChat(int page, int size) {
        Pageable pageable = PageRequest.of(
                safePage(page),
                safeSize(size),
                Sort.by("sentAt").descending()
        );

        return chatMessageRepository.findBySenderType(SenderType.USER, pageable)
                .map(this::mapAdminChat);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminStorageResponseDTO> getAllStorage(int page, int size) {
        Pageable pageable = PageRequest.of(
                safePage(page),
                safeSize(size),
                Sort.by("usedQuota").descending()
        );

        return cloudStorageRepository.findAll(pageable)
                .map(this::mapAdminStorage);
    }

    private StatusPublicDoc parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return StatusPublicDoc.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái tài liệu không hợp lệ: " + status);
        }
    }

    private UserAccountResponseDTO mapUserAccount(User user) {
        UserAccountResponseDTO dto = new UserAccountResponseDTO();

        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(resolveFullName(user));
        dto.setAccountStatus(user.getAccountStatus());
        dto.setStatus(user.getAccountStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRole(user.getRole());

        return dto;
    }

    private AdminDocumentResponseDTO mapAdminDocument(Document document) {
        AdminDocumentResponseDTO dto = new AdminDocumentResponseDTO();

        dto.setDocumentId(document.getId());
        dto.setDocumentName(document.getDocumentName());
        dto.setFileType(document.getFileType() != null ? document.getFileType().name() : null);
        dto.setFileSize(document.getFileSize());
        dto.setPreviewUrl(document.getPreviewUrl());
        dto.setDownloadUrl(document.getDownloadUrl());
        dto.setIsPublic(document.isPublic());
        dto.setStatus(document.getStatus());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setDescription(document.getDescription());
        dto.setCategoryId(document.getCategoryId());

        if (document.getUser() != null) {
            dto.setUserId(document.getUser().getId());
            dto.setUserEmail(document.getUser().getEmail());
            dto.setUserFullName(resolveFullName(document.getUser()));
            dto.setUserRole(
                    document.getUser().getRole() != null
                            ? document.getUser().getRole().name()
                            : null
            );
        }

        if (document.getApprovedBy() != null) {
            dto.setApprovedById(document.getApprovedBy().getId());
            dto.setApprovedByEmail(document.getApprovedBy().getEmail());
            dto.setApprovedByName(resolveFullName(document.getApprovedBy()));
        }

        return dto;
    }

    private AdminChatResponseDTO mapAdminChat(ChatMessage chatMessage) {
        AdminChatResponseDTO dto = new AdminChatResponseDTO();

        dto.setMessageId(chatMessage.getId());
        dto.setMessageContent(chatMessage.getMessageContent());
        dto.setSenderType(
                chatMessage.getSenderType() != null
                        ? chatMessage.getSenderType().name()
                        : null
        );
        dto.setSentAt(chatMessage.getSentAt());

        if (chatMessage.getChatSession() != null) {
            dto.setSessionId(chatMessage.getChatSession().getId());
            dto.setSessionTitle(chatMessage.getChatSession().getSessionTitle());

            if (chatMessage.getChatSession().getUser() != null) {
                User user = chatMessage.getChatSession().getUser();

                dto.setUserId(user.getId());
                dto.setUserEmail(user.getEmail());
                dto.setUserFullName(resolveFullName(user));
            }
        }

        return dto;
    }

    private AdminStorageResponseDTO mapAdminStorage(CloudStorage storage) {
        AdminStorageResponseDTO dto = new AdminStorageResponseDTO();

        dto.setStorageId(storage.getId());
        dto.setUsedQuota(storage.getUsedQuota());
        dto.setTotalQuota(storage.getTotalQuota());

        long usedQuota = storage.getUsedQuota() != null ? storage.getUsedQuota() : 0L;
        long totalQuota = storage.getTotalQuota() != null ? storage.getTotalQuota() : 0L;

        double percentage = totalQuota > 0
                ? usedQuota * 100.0 / totalQuota
                : 0.0;

        dto.setUsagePercentage(String.format("%.2f%%", percentage));

        if (storage.getUser() != null) {
            dto.setUserId(storage.getUser().getId());
            dto.setUserEmail(storage.getUser().getEmail());
            dto.setUserFullName(resolveFullName(storage.getUser()));
            dto.setUserRole(
                    storage.getUser().getRole() != null
                            ? storage.getUser().getRole().name()
                            : null
            );
        }

        return dto;
    }

    private String resolveFullName(User user) {
        if (user == null) {
            return null;
        }

        if (user.getCustomerProfile() != null
                && user.getCustomerProfile().getFullName() != null
                && !user.getCustomerProfile().getFullName().isBlank()) {
            return user.getCustomerProfile().getFullName();
        }

        if (user.getModeratorProfile() != null
                && user.getModeratorProfile().getFullName() != null
                && !user.getModeratorProfile().getFullName().isBlank()) {
            return user.getModeratorProfile().getFullName();
        }

        if (user.getAdminProfile() != null
                && user.getAdminProfile().getFullName() != null
                && !user.getAdminProfile().getFullName().isBlank()) {
            return user.getAdminProfile().getFullName();
        }

        return user.getEmail();
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return 10;
        }

        return Math.min(size, 100);
    }
}