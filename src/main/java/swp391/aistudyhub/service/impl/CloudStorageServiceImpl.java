package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.service.CloudStorageService;

import java.io.IOException;
import java.util.UUID;

@Service
public class CloudStorageServiceImpl implements CloudStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String anonKey;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";

        // Tạo tên file ngẫu nhiên bằng UUID để tránh trùng tên trên Supabase
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // URL REST API của Supabase Storage
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + anonKey);
            headers.set("apiKey", anonKey);
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            // Bắn dữ liệu thô lên Cloud
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                // Trả về link Public trực tiếp của file để lưu vào database
                return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + uniqueFileName;
            } else {
                throw new RuntimeException("Supabase trả về mã lỗi: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file nhị phân: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối Supabase Cloud: " + e.getMessage());
        }
    }
}