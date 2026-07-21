package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.dto.projection.ChatRequestResponse;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.StorageUsageResponse;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.request.ApprovePublicRequestDTO;
import swp391.aistudyhub.dto.request.MemberConfigDTO;
import swp391.aistudyhub.dto.request.SystemConfigDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.SubscriptionPlan;
import swp391.aistudyhub.entity.SystemConfig;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.*;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.AdminService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CloudStorageRepository cloudStorageRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;


    @Override
    public Page<UserAccountResponse> getAllCustomer(String key, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (key != null && !key.isEmpty()) {
            return userRepository.searchCustomers(key, pageable);
        }
        return userRepository.findBy(pageable);
    }

    @Override
    public UserAccountResponse updateUserStatus(UUID id, AccountStatus status) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new RuntimeException("This user is not exist!"));

        userRepository.updateUserStatus(id, status);

        user.setAccountStatus(status);
        return userRepository.findProjectedById(id);
    }

    @Override
    public Page<DocumentResponse> getAllDocument(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return documentRepository.findBy(pageable);
    }

    @Override
    public UserAccountResponse updateUserRole(UUID id, UserRole role) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new RuntimeException("This user is not exist!"));

        userRepository.updateUserRole(id, role);

        user.setRole(role);
        return userRepository.findProjectedById(id);
    }

    @Override
    public Page<ChatRequestResponse> getAllChat(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return chatMessageRepository.findBySenderType(pageable, SenderType.USER);
    }

    @Override
    public Page<StorageUsageResponse> getAllStorage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("usedQuota").descending());
        return cloudStorageRepository.findBy(pageable);

    }

    @Override
    public void systemConfig(SystemConfigDTO dto) {
        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("This config is not available"));
        systemConfig.setMaxDailyChatTokens(dto.getMaxDailyChatTokens());
        systemConfig.setTotalStorageQuotaGb(dto.getTotalStorageQuotaGb());
        systemConfig.setMaxFileSizeMb(dto.getMaxFileSizeMb());
        systemConfig.setAllowedFileTypes(dto.getAllowedFileTypes());
        systemConfigRepository.save(systemConfig);
    }

    @Override
    public void memberConfig(MemberConfigDTO dto) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("This subscription is not available"));
        subscriptionPlan.setMaxDailyChatTokens(dto.getMaxDailyChatTokens());
        subscriptionPlan.setMaxFileSizeMb(dto.getMaxFileSizeMb());
        subscriptionPlan.setTotalStorageQuotaGb(dto.getTotalStorageQuotaGb());
        subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    public void updatePriceMember(BigDecimal price) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(()-> new RuntimeException("This subscription is not available"));
        subscriptionPlan.setPrice(price);
        subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    public void approvePublicDocument(ApprovePublicRequestDTO dto) {
        User user = getCurrentUser();
        Document document = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("This document is not found!"));
        if(dto.getRqd() == RequestPublicDoc.ACCEPT) {
            document.setPublic(true);
            document.setStatus(StatusPublicDoc.SUCCESS);
            document.setApprovedBy(user);
        } else {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.DEFAULT);
        }

        documentRepository.save(document);
    }

    @Override
    public Page<DocumentResponse> getAllDocumentPending(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return documentRepository.findByPending(pageable);
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("You are not login yet!");
        }

        return authentication;
    }

    private User getCurrentUser() {
        Authentication authentication = getAuthentication();

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
    }
}
