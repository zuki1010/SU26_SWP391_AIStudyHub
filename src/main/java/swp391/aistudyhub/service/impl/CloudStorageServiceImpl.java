package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.dto.response.CloudStorageUsageResponseDTO;
import swp391.aistudyhub.entity.CloudStorage; // Đảm bảo đã import Entity này
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.service.CloudStorageService;

import java.io.IOException;
import java.util.UUID;


@Service
public class CloudStorageServiceImpl implements CloudStorageService {

    @Autowired
    private CloudStorageRepository cloudStorageRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String anonKey;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public String uploadFile(UUID userId, MultipartFile file, String description) {

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng cung cấp mô tả cho tài liệu trước khi upload!");
        }
        // 1. BỔ SUNG: Lấy thông tin cấu hình Storage của User từ DB local lên kiểm tra trước
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình bộ nhớ của người dùng"));

        // 2. BỔ SUNG: Kiểm tra xem file mới lên có làm tràn bộ nhớ (Vượt 15GB) không
        long fileSize = file.getSize();
        if (storage.getUsedQuota() + fileSize > storage.getTotalQuota()) {
            throw new RuntimeException("Dung lượng bộ nhớ đám mây của bạn đã đầy (Giới hạn tối đa 5GB)!");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";

        // Tối ưu: Bỏ .toString() thừa để code sạch hơn
        // ======================== SỬA ĐỔI ĐOẠN NÀY ========================
        // 1. Thay đổi: Thêm userId vào trước tên file để tạo cấu trúc thư mục chuẩn trên Supabase
        String uniqueFileName = userId + "/" + UUID.randomUUID() + fileExtension;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + anonKey);

            // 2. Sửa lỗi chính tả: Đổi 'apiKey' (chữ K viết hoa) thành 'apikey' (chữ k viết thường) theo luật của Supabase REST API
            headers.set("apikey", anonKey);

            String contentType = file.getContentType();
            headers.setContentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {

                // 3. Sửa đổi: Thêm chính xác cụm '/public/' vào đường dẫn trả về để tạo Link xem/tải trực tiếp công khai
                return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + uniqueFileName;
            } else {
                throw new RuntimeException("Supabase trả về mã lỗi: " + response.getStatusCode());
            }
            // ===================================================================
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file nhị phân: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối Supabase Cloud: " + e.getMessage());
        }
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public CloudStorageUsageResponseDTO getCloudStorageUsage(UUID userId) {
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình bộ nhớ của người dùng"));

        long realUsedQuota = documentRepository.sumFileSizeByUserId(userId);

        String percentageWithSign = "0%"; // Giá trị mặc định nếu chưa dùng gì

        if (storage.getTotalQuota() > 0 && realUsedQuota > 0) {
            double percentage = (realUsedQuota * 100.0) / storage.getTotalQuota();

            percentageWithSign = String.format("%.5f%%", percentage);
            if (percentage >= 0.01) {
                percentageWithSign = String.format("%.2f%%", percentage);
            }
        }

        return new CloudStorageUsageResponseDTO(
                userId,
                realUsedQuota,
                storage.getTotalQuota(),
                percentageWithSign
        );
    }
}