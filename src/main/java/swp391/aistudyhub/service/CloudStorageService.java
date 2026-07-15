package swp391.aistudyhub.service;

import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.dto.response.CloudStorageUsageResponseDTO;

public interface CloudStorageService {

    String uploadFile(MultipartFile file);

    CloudStorageUsageResponseDTO getCloudStorageUsage();
}