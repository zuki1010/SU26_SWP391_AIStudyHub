package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.StorageUploadLog;
import swp391.aistudyhub.repository.StorageUploadLogRepository;
import swp391.aistudyhub.service.StorageUploadService;

@Service
public class StorageUploadServiceImpl implements StorageUploadService {

    @Autowired
    private StorageUploadLogRepository storageUploadLogRepository;

    @Override
    @Transactional
    public void logSuccess(CloudStorage storage, String fileName, Long fileSize) {
        StorageUploadLog log = new StorageUploadLog();
        log.setCloudStorage(storage);
        log.setFileNameOrigin(fileName);
        log.setFileSize(fileSize);
        log.setUploadStatus("SUCCESS");

        storageUploadLogRepository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 🌟 Tạo transaction mới để không bị rollback theo luồng chính
    public void logFailure(CloudStorage storage, String fileName, Long fileSize, String status) {
        StorageUploadLog log = new StorageUploadLog();
        log.setCloudStorage(storage);
        log.setFileNameOrigin(fileName);
        log.setFileSize(fileSize);
        log.setUploadStatus(status);

        storageUploadLogRepository.saveAndFlush(log);
    }
}