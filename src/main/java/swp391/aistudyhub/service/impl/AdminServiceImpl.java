package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.projection.ChatRequestResponse;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.StorageUsageResponse;
import swp391.aistudyhub.dto.projection.SubscriptionPlanResponse;
import swp391.aistudyhub.dto.projection.SystemConfigResponse;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.request.ApprovePublicRequestDTO;
import swp391.aistudyhub.dto.request.MemberConfigDTO;
import swp391.aistudyhub.dto.request.SystemConfigDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.SubscriptionPlan;
import swp391.aistudyhub.entity.SystemConfig;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.RequestPublicDoc;
import swp391.aistudyhub.enums.SenderType;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.repository.ChatMessageRepository;
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.SubscriptionPlanRepository;
import swp391.aistudyhub.repository.SystemConfigRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.AdminService;
import swp391.aistudyhub.service.MailService;

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

    @Autowired
    private MailService mailService;

    @Override
    public Page<UserAccountResponse> getAllCustomer(String key, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (key != null && !key.trim().isEmpty()) {
            return userRepository.searchCustomers(key.trim(), pageable);
        }

        return userRepository.findBy(pageable);
    }

    @Override
    @Transactional
    public UserAccountResponse updateUserStatus(UUID id, AccountStatus status) {
        if (status == null) {
            throw new RuntimeException("Trạng thái tài khoản không hợp lệ.");
        }

        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Không được thay đổi trạng thái tài khoản ADMIN.");
        }

        user.setAccountStatus(status);
        userRepository.save(user);

        return userRepository.findProjectedById(id);
    }

    @Override
    @Transactional
    public UserAccountResponse updateUserRole(UUID id, UserRole role) {
        if (role == null) {
            throw new RuntimeException("Role không hợp lệ.");
        }

        if (role == UserRole.ADMIN) {
            throw new RuntimeException("Không được đổi quyền thành ADMIN từ giao diện quản trị.");
        }

        User user = userRepository.findUserById(id)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));

        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Không được thay đổi quyền của tài khoản ADMIN.");
        }

        UserRole oldRole = user.getRole();

        if (oldRole == role) {
            return userRepository.findProjectedById(id);
        }

        user.setRole(role);
        userRepository.save(user);

        mailService.sendRoleChangedEmail(user.getEmail(), role.name());

        return userRepository.findProjectedById(id);
    }

    @Override
    public Page<DocumentResponse> getAllDocument(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return documentRepository.findAllAdminDocuments(pageable);
    }

    @Override
    public Page<DocumentResponse> getAllDocument(int page, int size, StatusPublicDoc status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status == null) {
            return documentRepository.findAllAdminDocuments(pageable);
        }

        return documentRepository.findAdminDocumentsByStatus(status, pageable);
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
    @Transactional
    public void systemConfig(SystemConfigDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Dữ liệu cấu hình không hợp lệ.");
        }

        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Cấu hình hệ thống hiện chưa khả dụng."));

        if (dto.getMaxDailyChatTokens() != null) {
            if (dto.getMaxDailyChatTokens() < 0) {
                throw new RuntimeException("Giới hạn token không hợp lệ.");
            }

            systemConfig.setMaxDailyChatTokens(dto.getMaxDailyChatTokens());
        }

        if (dto.getTotalStorageQuotaGb() != null) {
            if (dto.getTotalStorageQuotaGb() <= 0) {
                throw new RuntimeException("Tổng dung lượng lưu trữ phải lớn hơn 0GB.");
            }

            systemConfig.setTotalStorageQuotaGb(dto.getTotalStorageQuotaGb());
        }

        if (dto.getMaxFileSizeMb() != null) {
            if (dto.getMaxFileSizeMb() <= 0) {
                throw new RuntimeException("Dung lượng tối đa mỗi file phải lớn hơn 0MB.");
            }

            systemConfig.setMaxFileSizeMb(dto.getMaxFileSizeMb());
        }

        if (dto.getAllowedFileTypes() != null) {
            systemConfig.setAllowedFileTypes(dto.getAllowedFileTypes().trim().toLowerCase());
        }

        systemConfigRepository.save(systemConfig);
    }

    @Override
    @Transactional
    public void memberConfig(MemberConfigDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Dữ liệu cấu hình Premium không hợp lệ.");
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Gói Premium hiện chưa khả dụng."));

        if (dto.getMaxDailyChatTokens() != null) {
            if (dto.getMaxDailyChatTokens() < 0) {
                throw new RuntimeException("Giới hạn token Premium không hợp lệ.");
            }

            subscriptionPlan.setMaxDailyChatTokens(dto.getMaxDailyChatTokens());
        }

        if (dto.getTotalStorageQuotaGb() != null) {
            if (dto.getTotalStorageQuotaGb() <= 0) {
                throw new RuntimeException("Dung lượng lưu trữ Premium phải lớn hơn 0GB.");
            }

            subscriptionPlan.setTotalStorageQuotaGb(dto.getTotalStorageQuotaGb());
        }

        if (dto.getMaxFileSizeMb() != null) {
            if (dto.getMaxFileSizeMb() <= 0) {
                throw new RuntimeException("Dung lượng tối đa mỗi file Premium phải lớn hơn 0MB.");
            }

            subscriptionPlan.setMaxFileSizeMb(dto.getMaxFileSizeMb());
        }

        subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    @Transactional
    public void updatePriceMember(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Giá Premium không hợp lệ.");
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Gói Premium hiện chưa khả dụng."));

        subscriptionPlan.setPrice(price);

        subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    @Transactional
    public void approvePublicDocument(ApprovePublicRequestDTO dto) {
        if (dto == null || dto.getDocumentId() == null || dto.getRqd() == null) {
            throw new RuntimeException("Dữ liệu duyệt tài liệu không hợp lệ.");
        }

        User reviewer = getCurrentUser();

        Document document = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu cần duyệt."));

        if (document.getStatus() != StatusPublicDoc.PENDING) {
            throw new RuntimeException("Tài liệu này không có yêu cầu public đang chờ duyệt.");
        }

        if (dto.getRqd() == RequestPublicDoc.ACCEPT) {
            document.setPublic(true);
            document.setStatus(StatusPublicDoc.SUCCESS);
            document.setApprovedBy(reviewer);
        } else if (dto.getRqd() == RequestPublicDoc.DENY) {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.DENY);
            document.setApprovedBy(reviewer);
        } else {
            throw new RuntimeException("Quyết định phê duyệt không hợp lệ.");
        }

        Document savedDocument = documentRepository.saveAndFlush(document);

        if (savedDocument.getUser() != null && savedDocument.getUser().getEmail() != null) {
            mailService.sendDocumentReviewResultEmail(
                    savedDocument.getUser().getEmail(),
                    savedDocument.getDocumentName(),
                    dto.getRqd().name()
            );
        }

        mailService.sendDocumentReviewConfirmationEmail(
                reviewer.getEmail(),
                savedDocument.getDocumentName(),
                dto.getRqd().name()
        );
    }

    @Override
    public SystemConfigResponse getSystemConfig() {
        return systemConfigRepository.findBy();
    }

    @Override
    public SubscriptionPlanResponse getSubscriptionConfig() {
        return subscriptionPlanRepository.findBy();
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("Vui lòng đăng nhập để tiếp tục.");
        }

        return authentication;
    }

    private User getCurrentUser() {
        Authentication authentication = getAuthentication();

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hiện tại."));
    }
}