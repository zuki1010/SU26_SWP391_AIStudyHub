package swp391.aistudyhub.service.impl;

import org.springframework.stereotype.Service;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.service.StorageUploadService;

@Service
public class StorageUploadServiceImpl implements StorageUploadService {

    @Override
    public void logSuccess(CloudStorage storage, String fileName, Long fileSize) {
        System.out.println(
                "[STORAGE_UPLOAD_SUCCESS] userId="
                        + (storage != null && storage.getUser() != null ? storage.getUser().getId() : null)
                        + ", fileName=" + fileName
                        + ", fileSize=" + fileSize
        );
    }

    @Override
    public void logFailure(CloudStorage storage, String fileName, Long fileSize, String reason) {
        System.out.println(
                "[STORAGE_UPLOAD_FAILED] userId="
                        + (storage != null && storage.getUser() != null ? storage.getUser().getId() : null)
                        + ", fileName=" + fileName
                        + ", fileSize=" + fileSize
                        + ", reason=" + reason
        );
    }
}