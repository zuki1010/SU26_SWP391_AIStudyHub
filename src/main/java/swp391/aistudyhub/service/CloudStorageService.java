package swp391.aistudyhub.service;

import org.springframework.web.multipart.MultipartFile;


public interface CloudStorageService {
    String uploadFile(MultipartFile file);
}
