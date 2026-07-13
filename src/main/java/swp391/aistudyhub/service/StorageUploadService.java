package swp391.aistudyhub.service;

import swp391.aistudyhub.entity.CloudStorage;

public interface StorageUploadService {
    void logSuccess(CloudStorage storage, String fileName, Long fileSize);
    void logFailure(CloudStorage storage, String fileName, Long fileSize, String status);
}