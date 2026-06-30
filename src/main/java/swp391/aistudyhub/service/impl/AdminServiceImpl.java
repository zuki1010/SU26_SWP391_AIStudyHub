package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import swp391.aistudyhub.dto.projection.ChatRequestResponse;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.StorageUsageResponse;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.SenderType;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.AdminService;

import java.util.List;
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
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdAt").descending());
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
        Pageable pageable = PageRequest.of(page,size, Sort.by("sentAt").descending());
        return chatMessageRepository.findBySenderType(pageable, SenderType.USER);
    }

    @Override
    public Page<StorageUsageResponse> getAllStorage(int page, int size) {
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdAt").descending());
        return cloudStorageRepository.findBy(pageable);
    }
}
