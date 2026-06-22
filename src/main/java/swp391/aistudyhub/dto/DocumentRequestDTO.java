package swp391.aistudyhub.dto;

import lombok.Data;

@Data
public class DocumentRequestDTO {
    private String documentName;
    private String fileType;
    private String previewUrl;
    private String downloadUrl;
    private Long fileSize; // Dung lượng file tính bằng Byte để check quota bộ nhớ

    // ĐÃ BỔ SUNG: Chuỗi văn bản thô trích xuất từ file do Frontend gửi lên để xử lý RAG Chunking
    private String textContent;
}