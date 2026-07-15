package swp391.aistudyhub.dto.projection;

import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.repository.AdminProfileRepository;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.repository.ModeratorProfileRepository;
import swp391.aistudyhub.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

public interface DocumentResponse {

    String getDocumentName();

    Long getFileSize();

    Instant getCreatedAt();

    UUID getUserId();

    default String getUserEmail(UserRepository userRepo) {
        if (getUserId() == null) {
            return "N/A";
        }

        return userRepo.findUserById(getUserId())
                .map(user -> user.getEmail())
                .orElse("N/A");
    }

    default String getUserName(
            UserRepository userRepo,
            CustomerProfileRepository customerRepo,
            ModeratorProfileRepository moderatorRepo,
            AdminProfileRepository adminRepo
    ) {
        if (getUserId() == null) {
            return "Unknown";
        }

        return userRepo.findById(getUserId())
                .map(user -> {
                    UserRole role = user.getRole();

                    if (role == UserRole.CUSTOMER) {
                        return customerRepo.findByUser_Id(user.getId())
                                .map(cp -> cp.getFullName())
                                .orElse("Unknown Customer");
                    }

                    if (role == UserRole.MODERATOR) {
                        return moderatorRepo.findByUser_Id(user.getId())
                                .map(mp -> mp.getFullName())
                                .orElse("Unknown Moderator");
                    }

                    if (role == UserRole.ADMIN) {
                        return adminRepo.findByUser_Id(user.getId())
                                .map(ap -> ap.getFullName())
                                .orElse("Unknown Admin");
                    }

                    return "Unknown Role";
                })
                .orElse("User Not Found");
    }
}