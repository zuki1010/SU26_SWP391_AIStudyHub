package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.dto.response.CloudStorageUsageResponseDTO;
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.CloudStorageService;

import java.io.IOException;
import java.util.UUID;

@Service
public class CloudStorageServiceImpl implements CloudStorageService {

    @Autowired
    private CloudStorageRepository cloudStorageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String anonKey;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    UserMemberSubscriptionRepository userMemberSubscriptionRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    @Transactional
    public String uploadFile(MultipartFile file) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình bộ nhớ của người dùng"));

        long fileSize = file.getSize();

        UserMemberSubscription userMemberSubscription = userMemberSubscriptionRepository.findByUser(user)
                .orElse(null);

        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("This config is not available"));

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("This subscription is not available"));

        Double totalQuota;

        if (userMemberSubscription == null) {
            totalQuota = systemConfig.getTotalStorageQuotaGb();
        } else {
            totalQuota = subscriptionPlan.getTotalStorageQuotaGb();
        }

        if (storage.getUsedQuota() + fileSize > totalQuota * 1073741824) {
            throw new RuntimeException("Dung lượng bộ nhớ đám mây của bạn đã đầy!");
        }

        String originalFilename = file.getOriginalFilename();

        String fileExtension =
                originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".file";

        String uniqueFileName = userId + "/" + UUID.randomUUID() + fileExtension;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + uniqueFileName;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + anonKey);
            headers.set("apikey", anonKey);

            String contentType = file.getContentType();

            headers.setContentType(
                    contentType != null
                            ? MediaType.parseMediaType(contentType)
                            : MediaType.APPLICATION_OCTET_STREAM
            );

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return supabaseUrl
                        + "/storage/v1/object/public/"
                        + bucketName
                        + "/"
                        + uniqueFileName;
            }

            throw new RuntimeException("Supabase trả về mã lỗi: " + response.getStatusCode());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file nhị phân: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối Supabase Cloud: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CloudStorageUsageResponseDTO getCloudStorageUsage() {
        User user = getCurrentUser();
        UUID userId = user.getId();

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình bộ nhớ của người dùng"));

        Double realUsedQuota = documentRepository.sumFileSizeByUserId(userId);

        String percentageWithSign = "0%";

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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("You are not login yet!");
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
    }
}