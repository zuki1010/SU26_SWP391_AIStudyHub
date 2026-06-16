package swp391.aistudyhub.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface CloudStorageService {
    String uploadFile(UUID userId, MultipartFile file);
}