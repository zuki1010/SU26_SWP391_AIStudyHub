package swp391.aistudyhub.service;

import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.dto.response.CloudStorageUsageResponseDTO;

import java.util.UUID;

public interface CloudStorageService {
    String uploadFile(UUID userId, MultipartFile file);
    CloudStorageUsageResponseDTO getCloudStorageUsage(UUID userId);
}